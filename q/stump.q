
//Build decision stump.

classifyStump:{[cla]
	a:`freq xdesc select from feature where keyword in (cla[`keyword]);
        a:a ij 1!select postid,yi,xweight from trsample;
        a:select postid,keyword,freq,yi,xweight from a;
	a:update ht:-1 from a;

	sent: cla[`sentiment];
	if[sent=-1;a:update ht:1 from a where freq<cla[`threshold];];
	if[sent=1;a:update ht:1 from a where freq>cla[`threshold];];

	a:update htyi:ht*yi from a;
	:a
	}

updateWeightStump:{[cl;alpha]
	/we need postid,freq,yi,xweight cols.
	a:classifyStump[cl];
	a:update xweight:recalcWeight[xweight;alpha;htyi] from a;
        res:1!select postid,freq,yi,xweight from a;
        :res
	}

//updateWeightStumpL
updateWeightStumpL:{[cl;alpha]
	/we need postid,freq,yi,xweight cols.
        a:classifyStump[cl];
        a:update xweight:recalcWeightL[xweight;alpha;htyi] from a;
        res:1!select postid,freq,yi,xweight from a;
        :res
	}

//For negative keywords, use lt.
stumpN:{[feat]
	a:`freq xdesc select from feature where keyword in (feat);
	a:a ij 1!select postid,yi,xweight from trsample;
	a:select postid,keyword,freq,yi,xweight from a;
	
	freqs: exec freq from a;

	sampleCnt:count a;
	cnt:0;
	minErr:100000;
	threshold:0.0;
	do[sampleCnt;
		a:update cl:-1 from a;
		a:update cl:1 from a where freq<freqs[cnt];
		a:update clyi:cl*yi from a;
		err:exec sum xweight from a where clyi=-1;
		if[err<minErr; minErr:err; threshold:freqs[cnt]];
		cnt:cnt+1;
	];	
	:(feat; threshold; minErr; -1)
	}

//Use gt for positive keywords.
stumpP:{[feat]
	a:`freq xdesc select from feature where keyword in (feat);
	a:a ij 1!select postid,yi,xweight from trsample;
        a:select postid,keyword,freq,yi,xweight from a;

	freqs: exec freq from a;
	
	sampleCnt: count a;
	cnt:0;
	minErr:100000.0;
	threshold:0;
	do[sampleCnt;
        	a:update cl:-1 from a;
        	a:update cl:1 from a where freq>freqs[cnt];
        	a:update clyi:cl*yi from a;
        	err:exec sum xweight from a where clyi=-1;
		if[err<minErr; minErr:err; threshold:freqs[cnt]];
		cnt:cnt+1;
	];

	//return feat,threshold,and err
	:(feat; threshold; minErr; 1)
	}

bestStumpP:{
	feats: distinct exec keyword from feature where sentiment=1;
	cnt:0;
	stump: (`;0;0; 1);
	minErr:1000000.0;
	do[count feats;
		tmp: stumpP[feats[cnt]];
		if[tmp[2]<minErr; minErr:tmp[2]; stump: tmp];
		cnt:cnt+1;
	];
	:stump
	}

bestStumpN:{
        feats: distinct exec keyword from feature where sentiment=-1;
        cnt:0;
        stump: (`;0;0; -1);
        minErr:1000000.0;
        do[count feats;
                tmp: stumpN[feats[cnt]];
                if[tmp[2]<minErr; minErr:tmp[2]; stump: tmp];
                cnt:cnt+1;
        ];
        :stump
        }

getBestStump:{
	bestn:bestStumpN[];
	bestp:bestStumpP[];
	res:bestp;
	if[bestn[2]<bestp[2]; res:bestn];
	:res
	}

\
//1mdb is the feature.
a:`freq xdesc select from feature where keyword in (`1mdb)
b:1!select postid,yi,xweight from trsample
a:a ij b
b:select postid,keyword,freq,yi,xweight from a
b:update cl:-1 from b
//10 is our threshold value
b:update cl:1 from b where freq<10
b:update clyi:cl*yi from b
//error weight.
select sum xweight from b where clyi=-1
