

import java.util.*;
import java.util.regex.*;

import java.net.*;
import java.io.*;
import java.text.*;
import java.lang.reflect.Array;

import com.restfb.*;
import com.restfb.types.*;

import kx.c;

public class GetID {

/*
	Date oneWeekAgo = new Date(currentTimeMillis() - 1000L * 60L * 60L * 24L * 7L);

	Connection<Post> filteredFeed = facebookClient.fetchConnection("me/feed", Post.class,
  			Parameter.with("limit", 3), Parameter.with("until", "yesterday"),
    			Parameter.with("since", oneWeekAgo));

	out.println("Filtered feed count: " + filteredFeed.getData().size());
*/

	/*
	args 0 : fbpage
	args 1 : no of posts
	*/
	public static void main(String[] args){
		String MYTOKEN = readToken();
		FacebookClient facebookClient = new DefaultFacebookClient(MYTOKEN);

                Connection<Post> myFeed = facebookClient.fetchConnection(args[0] + "/posts", Post.class, Parameter.with("limit",5));

		int npost = Integer.parseInt(args[1]);

		int msgCount=0;

		try{
        
                        PrintWriter outFile = new PrintWriter(new FileWriter(args[0] + "-" + msgCount +  "-xtract.sh"));
	
			outFile.println("#!/bin/bash");
			outFile.println("");

                for (List<Post> myFeedConnectionPage : myFeed){
                        for (Post post : myFeedConnectionPage){

			/*	if(msgCount>0 && msgCount%10==0) {
					outFile.close();
					outFile = new PrintWriter(new FileWriter( args[0] + "-" + msgCount +  "-xtract.sh"));
					outFile.println("#!/bin/bash");
                        		outFile.println("");
				}*/
                                String message = post.getMessage();
                                String id      = post.getId();

                                System.out.println("");
                                System.out.println("ID:   " + id + "  " + post.getCreatedTime());
                                System.out.println("Message: -->>> " + message );
                            
                                System.out.println("Message Count: " + msgCount);

				outFile.println("./xtracti.sh " + args[0] + " " + id);                            

				msgCount++;

                                if( msgCount >= npost ) {
					//writeToKdb();
					outFile.close();
					return;
				}
                        }
                }

		} catch(Exception e) {
                        System.err.println("Error: Cannot write output file");
                }
	}

	private static void generateScript(String page, String postid) {
		try{

                	PrintWriter outFile = new PrintWriter(new FileWriter(postid+page+ "-output.csv"));

			outFile.close();

                } catch(Exception e) {
                        System.err.println("Error: Cannot write output file");
                }
	}

	private static String readToken() {
                String TOKEN = "";
                try{
                        InputStream fis=new FileInputStream("token.txt");
                        BufferedReader br=new BufferedReader(new InputStreamReader(fis));
                        TOKEN = br.readLine();

                }
                catch(Exception e){
                        System.err.println("Error: Token File Cannot Be Read");
                }

                System.out.println("Token: " + TOKEN);

                return TOKEN;
        }

	private static void writeToKdb() {
		
		try{
 			c c=new c("localhost",5015);
 			Object[]x={"najibrazak","xxyyyyy","syabas",new Integer(1), new Integer(33) };

 			c.ks("insert","feature",x);
        
        		c.close();
		}catch(Exception e){
        		e.printStackTrace();
		}
	}

}
