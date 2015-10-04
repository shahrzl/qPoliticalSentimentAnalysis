
import java.util.*;
import java.util.regex.*;

import java.io.*;
import java.net.*;
import java.text.*;
import java.lang.reflect.Array;

import com.restfb.*;
import com.restfb.types.*;

import kx.c;

public class FXtractI {

	private static Map<String, Integer> wordCount = new TreeMap<>();

	private static Map<String, Integer> posFeat = new TreeMap<>();
	private static Map<String, Integer> negFeat = new TreeMap<>();

	private static Map<String, String> posMeta = new TreeMap<>();
        private static Map<String, String> negMeta = new TreeMap<>();

	private static c qConn = null;

	/*
	args 0: pos features
	args 1:neg features
	args 2: fbpage
	args 3: fb post ID
	args 4: print wordcount flag y,n
	args 5: print feature flag y,n
	*/
	public static void main(String[] args) {

		String MYTOKEN = readToken();

		readFeatures(args[0], posFeat, posMeta);
		readFeatures(args[1], negFeat, negMeta);

		String fbpage = args[2];
		String fbpostid = args[3];
		String printflg = args[4];
		String printfeat = args[5];

		FacebookClient facebookClient = new DefaultFacebookClient(MYTOKEN);
		
		//Open connection to Kdb+
		openQConn("localhost", 5015);

		System.out.println("");
		System.out.println("ID:   " + fbpostid);
					
		getCommentFromPost(facebookClient, fbpostid, false);
		
		if("y".equals(printflg)) printWordCount();
		
		System.out.println("");				
		System.out.println("Aggregating features...");
                aggregateFeatures();
                                
		if("y".equals(printfeat)) printFeatures();
				
		System.out.println("Writing data to kdb....");
		writeFeaturesToKdb(fbpage, fbpostid);                                
                clearCounts();

		closeQConn();
		System.out.println("");
	}

	private static String readToken() {
                String TOKEN = "";
                try{
                        InputStream fis=new FileInputStream("token.txt");
                        BufferedReader br=new BufferedReader(new InputStreamReader(fis));
                        TOKEN = br.readLine();
			br.close();
                }
                catch(Exception e){
                        System.err.println("Error: Token File Cannot Be Read");
                }

                System.out.println("Token: " + TOKEN);

                return TOKEN;
        }

	private static void readFeatures(String filename, Map feature, Map meta) {
		
		String separator = ",";		
		
		System.out.println("feature file: "+ filename);

		try{
                        InputStream fis=new FileInputStream(filename);
                        BufferedReader br=new BufferedReader(new InputStreamReader(fis));
                        for (String line = br.readLine(); line != null; line = br.readLine()) {
       			//	System.out.println(line);
				String[] tokens = line.split(separator);

				feature.put(tokens[0], 0 );
				meta.put(tokens[0], tokens[1]);
    			}

    			br.close();

                }
                catch(Exception e){
                        System.err.println("Error: Feature File Cannot Be Read" + e.toString());
                }

	}

	private static void  getCommentFromPost(FacebookClient client, String post_id, boolean recur){
	
				

    		Connection<Comment> allComments = client.fetchConnection(post_id+"/comments", Comment.class, Parameter.with("limit",100));
    		for(List<Comment> postcomments : allComments){
        		for (Comment comment : postcomments){
//        			System.out.println("Comment:#####  " + comment.getMessage());
				System.out.print(".");
				parseComment(comment.getMessage());
				if(recur) getCommentFromPost(client, comment.getId(), false);
        		}
    		}
	}

	private static void parseComment(String comment) {

        	//String separator = " !?.,#='()-\"\r\n";
		String separator = " !?.,#+-;/&@~:='()-\"\r\n";

        	StringTokenizer st = new StringTokenizer( comment, separator, true );

        	while ( st.hasMoreTokens() ) {
            		String token = st.nextToken();
            		if ( token.length() == 1 && separator.indexOf( token.charAt( 0 ) ) >= 0 ) {
    //            		System.out.println( "special char:" + token );
            		}
            		else {
  //              		System.out.println( "word :" + token );
				
				wordCounter(token.toLowerCase().trim());
            		}

        	}
	}

	/*


	*/
	private static void wordCounter(String word){

		if (wordCount.containsKey(word)) {
        	// Map already contains the word key. Just increment it's count by 1
        		wordCount.put(word, wordCount.get(word) + 1);
    		} else {
        	// Map doesn't have mapping for word. Add one with count = 1
        		wordCount.put(word, 1);
    		}
	}

	private static void printWordCount(){
		
		wordCount.forEach((k, v) ->  
			{
			System.out.println(k + "=" + v);
			}
			);
	}

	private static void aggregateFeatures(){
		
		posFeat.forEach((k,v) ->
			{
			wordCount.forEach((k2,v2) ->
				{
				if(Pattern.matches(k, k2)) posFeat.put(k, posFeat.get(k) + v2);
				}	
				);
			}
			);

		negFeat.forEach((k,v) ->
                        {
                        wordCount.forEach((k2,v2) ->
                                {
                                if(Pattern.matches(k, k2)) negFeat.put(k, negFeat.get(k) + v2);
                                }
                                );
                        }
                        );

	}

	private static void printFeatures(){
		
		System.out.println("");
		System.out.println("Positive features: ");
		posFeat.forEach((k, v) ->
                        {
                        	System.out.println( posMeta.get(k) + "=" + v);
                        }
                        );

		System.out.println("");
		System.out.println("Negative features: ");
		negFeat.forEach((k, v) ->
                        {
                       		System.out.println( negMeta.get(k) + "=" + v);
                        }
                        );
	}

	private static void writeFeatures(String postID) {
	
		try{

		PrintWriter outFile = new PrintWriter(new FileWriter(postID + "-output.csv"));

		//for(i=0;i<10;i++)
    		//	f0.println("Result "+ i +" : "+ ans);
		//f0.close();
				
		posFeat.forEach((k, v) ->
                        {
                                System.out.println( postID + "," + posMeta.get(k) + "," + v + "," + "1");
				outFile.println( postID + "," + posMeta.get(k) + "," + v + "," + "1" );
                        }
                        );

                negFeat.forEach((k, v) ->
                        {
                                System.out.println( postID + "," + negMeta.get(k) + "," + v + "," + "-1");
				outFile.println( postID + "," + negMeta.get(k) + "," + v + "," + "1" );
                        }
                        );

		outFile.close();

		} catch(Exception e) {
			System.err.println("Error: Cannot write output file");
		}
	}

	private static void openQConn(String host, int port) {
		try{
			qConn = new c(host,port);	
		}
		catch(Exception e){
                        e.printStackTrace();
                }

	}

	private static void closeQConn() {
		try{
                        qConn.close();       
                }
                catch(Exception e){
                        e.printStackTrace();
                }

	}

	private static void writeFeaturesToKdb(String fbpage, String postid) {
		
		try{
                        //c c=new c("localhost",5015);
                        //Object[]x={"mypage","xxyyyyy","syabas",new Integer(1), new Integer(33) };
			//c.ks("insert","feature",x);
			
			posFeat.forEach((k, v) ->
                        {
                                //System.out.println( postid + "," + posMeta.get(k) + "," + v + "," + "1");
				Object[]x={ fbpage , postid , posMeta.get(k) , new Integer(1) , new Integer(v) };
                        	try{
					qConn.ks("insert","feature",x);
				}catch(IOException ioe) {
                        		ioe.printStackTrace();
                		}
                        }
                        );

                	negFeat.forEach((k, v) ->
                        {
                                //System.out.println( postid + "," + negMeta.get(k) + "," + v + "," + "-1");
				Object[]x={ fbpage , postid , negMeta.get(k) , new Integer(-1) , new Integer(v) };
                                try{
                                        qConn.ks("insert","feature",x);
                                }catch(IOException ioe) {
                                        ioe.printStackTrace();
                                }
                        }
                        );

                        //c.close();
                }catch(Exception e){
                        e.printStackTrace();
                }

	}

	private static void clearCounts() {
		//clear wordCount
		wordCount.clear();

		//zeroed features count
		posFeat.forEach((k, v) ->
                        {
                                posFeat.put(k, 0);
                        }
                        );

                negFeat.forEach((k, v) ->
                        {
                                negFeat.put(k, 0);
                        }
                        );	
	} 




}
