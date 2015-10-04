//Automated sentiment analysis using Machine Learning algorithm.

\l stump.q

//Weak classifier
//for negative sentiment
htn:{
	a:`freq xdesc select from feature where sentiment=-1;
	a:update httmp:freq>=1 from a;
	a:update ht:-1 from a where httmp=1;
	a:update ht:1 from a where httmp=0;
	:select fbpage,postid,keyword,sentiment,ht from a
	}

//for positive sentiment
htp:{
	a:`freq xdesc select from feature where sentiment=1;
	a:update httmp:freq>=1 from a;
        a:update ht:1 from a where httmp=1;
        a:update ht:-1 from a where httmp=0;
        :select fbpage,postid,keyword,sentiment,ht from a
	}

label:{
	a:select sum[freq*sentiment] by postid from select sum freq by postid,sentiment from feature;
	a:update yi:-1 from a where freq<0;
	a:update yi:1 from a where freq>=0;
	:a
	}

initWeight:{[tbl]
	:update xweight:1%count tbl from tbl;
	}

//adaBoost function should update weight in trsample to new value in each round.
trsample:initWeight[label[]];
nclass:htn[];
pclass:htp[];

//turn freq to a weight
update freq:freq%sum freq by postid from `feature;

getpcnt:{
	:count select from trsample where yi=1
	}

getncnt:{
	:count select from trsample where yi=-1;
	}

/get classification err for negative keywords
nmis:{
	test: nclass ij trsample;
	nmisclassify: select from test where ht<>yi;
	:`xweight xasc select sum xweight by keyword from nmisclassify
	}

/get classifiation err for positive keywords
pmis:{
	test: pclass ij trsample;
	pmisclassify: select from test where ht<>yi;
	:`xweight xasc select sum xweight by keyword from pmisclassify
	}

getWeakC:{
	nm:nmis[];
	pm:pmis[];

	n: exec first keyword,first xweight from nm;
	nkey: n[`keyword];
	nw: n[`xweight];
	n[`tbl]:nclass;
	n[`sentiment]:-1;

	p: exec first keyword,first xweight from pm;
	pkey: p[`keyword];
        pw: p[`xweight];
	p[`tbl]:pclass;
	p[`sentiment]:1;
	
	res:n;
	if[nw<pw; res:n];
	if[nw>=pw; res:p];
	:res
	}

alpha:{[np]
	:0.5*log[(1-np[`xweight])%np[`xweight]];
	}

/adaBoost
recalcWeight:{[xw;alp;htyi]
	:xw*exp[-1*htyi*alp]
	}

/logitBoost
recalcWeightL:{[xw;alp;htyi]
	a:1+exp[htyi*alp];
	a:1%a;
	:xw*a
	}

classifyWeakLearner:{[cl]
	a:select from cl[`tbl]  where keyword in (cl[`keyword]);
	a:1!select postid,freq,yi,ht,xweight,htyi:ht*yi from a ij trsample;
	:a
	}

/adaBoost
/call updateWeight and set trsample with new weight.
updateWeight:{[cl;alpha]
	/we need postid,freq,yi,xweight cols.pkey is postid.
	a:classifyWeakLearner[cl];
	a:update xweight:recalcWeight[xweight;alpha;htyi] from a;
	res:1!select postid,freq,yi,xweight from a;
	:res
	}

/logitBoost
updateWeightL:{[cl;alpha]
	/we need postid,freq,yi,xweight cols.pkey is postid.
        a:classifyWeakLearner[cl];
        a:update xweight:recalcWeightL[xweight;alpha;htyi] from a;
        res:1!select postid,freq,yi,xweight from a;
        :res
	}


//Define table to store boosting result.
fclassifier:([] btype:`$(); rnd:`int$(); keyword:`$(); weight:`float$(); sentiment:`int$() );

fsclassifier:([] btype:`$(); rnd:`int$(); keyword:`$(); threshold:`float$(); weight:`float$(); sentiment:`int$() );

//A very simple version of stump where threshold is always 0.
qAdaBoost:{[T]
	cnt:0;
	//place code inside the do loop.
	do[T;
		cl:getWeakC[];
		alp:alpha[cl];	
		trsample::updateWeight[cl;alp];
		//normalize weight
		trsample::update xweight:xweight%sum xweight from trsample;
		//print out cl and alpha
		0N!`RoundNo;
		0N!cnt;
		0N!cl;
		0N!alp;

		insert[`fclassifier;(`adaBoost; cnt+1; cl[`keyword]; alp; cl[`sentiment])];

		cnt+:1;
	];

	}

//Use stump as weak classifier.
qAdaBoostS:{[T]
	cnt:0;
        //place code inside the do loop.
        do[T;
                tmp:getBestStump[];
		cl:`keyword`threshold`xweight`sentiment!tmp;
                alp:alpha[cl];
                trsample::updateWeightStump[cl;alp];
                //normalize weight
                trsample::update xweight:xweight%sum xweight from trsample;
                //print out cl and alpha
                0N!`RoundNo;
                0N!cnt;
                0N!cl;
                0N!alp;

                insert[`fsclassifier;(`adaBoost; cnt+1; cl[`keyword]; `float$cl[`threshold]; alp; cl[`sentiment])];

                cnt+:1;
        ];
	}

qLogitBoost:{[T]
	cnt:0;
        //place code inside the do loop.
        do[T;
                cl:getWeakC[];
                alp:alpha[cl];
                trsample::updateWeightL[cl;alp];
                //normalize weight
                trsample::update xweight:xweight%sum xweight from trsample;
                //print out cl and alpha
                0N!`RoundNo;
                0N!cnt;
                0N!cl;
                0N!alp;
                cnt+:1;
        ];


	}

qLogitBoostS:{[T]
	cnt:0;
        //place code inside the do loop.
        do[T;
                tmp:getBestStump[];
                cl:`keyword`threshold`xweight`sentiment!tmp;
                alp:alpha[cl];
                trsample::updateWeightStumpL[cl;alp];
                //normalize weight
                trsample::update xweight:xweight%sum xweight from trsample;
                //print out cl and alpha
                0N!`RoundNo;
                0N!cnt;
                0N!cl;
                0N!alp;

                insert[`fsclassifier;(`adaBoost; cnt+1; cl[`keyword]; `float$cl[`threshold]; alp; cl[`sentiment])];

                cnt+:1;
        ];
	}

\

Usage:

\l sentiment.q

Call qAdaBoost[T] where T is the no of rounds.
