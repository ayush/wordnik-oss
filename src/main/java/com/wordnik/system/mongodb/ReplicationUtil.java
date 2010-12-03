// Copyright (C) 2010  Wordnik, Inc.
//
// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or (at your 
// option) any later version.  This program is distributed in the hope that it
// will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty 
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser 
// General Public License for more details.  You should have received a copy 
// of the GNU Lesser General Public License along with this program.  If not,
// see <http://www.gnu.org/licenses/>.

package com.wordnik.system.mongodb;

import java.util.ArrayList;
import java.util.List;

public class ReplicationUtil extends MongoUtil {
	protected static String DATABASE_HOST = null;
	protected static String DATABASE_USER_NAME = null;
	protected static String DATABASE_PASSWORD = null;

	protected static String DEST_DATABASE_USER_NAME = null;
	protected static String DEST_DATABASE_PASSWORD = null;
	protected static String DEST_DATABASE_HOST = null;
	
	protected static String OPLOG_LAST_FILENAME = "last_timestamp.txt";
	protected static String COLLECTIONS_STRING;

	public static void main(String ... args){
		if(!parseArgs(args)){
			usage();
			return;
		}
		if(DATABASE_HOST == null){
			usage();
			return;
		}
		if(DEST_DATABASE_HOST == null){
			usage();
			return;
		}

		new ReplicationUtil().run();
	}

	protected void run(){
		//	create and configure a replication target processor
		OplogReplayWriter util = new OplogReplayWriter();
		util.setDestinationDatabaseUsername(DEST_DATABASE_USER_NAME);
		util.setDestinationDatabasePassword(DEST_DATABASE_PASSWORD);
		util.setDestinationDatabaseHost(DEST_DATABASE_HOST);

		try{
			//	create and configure a tail thread
			OplogTailThread thd = new OplogTailThread(util, DBConnector.getDb(DATABASE_HOST, DATABASE_USER_NAME, DATABASE_PASSWORD, "local"));
			List<String> inclusions = new ArrayList<String>();
			List<String> exclusions = new ArrayList<String>();
			selectCollections(COLLECTIONS_STRING, inclusions, exclusions);

			thd.setInclusions(inclusions);
			thd.setExclusions(exclusions);
			thd.start();

			StopFileMonitor mon = new StopFileMonitor(thd);
			mon.start();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	public static boolean parseArgs(String...args){
		for (int i = 0; i < args.length; i++) {
			switch (args[i].charAt(1)) {
			case 'U':
				DEST_DATABASE_USER_NAME = args[++i];
				break;
			case 'P':
				DEST_DATABASE_PASSWORD = args[++i];
				break;
			case 'H':
				DEST_DATABASE_HOST = args[++i];
				break;
			case 'c':
				COLLECTIONS_STRING = args[++i];
				break;
			case 'u':
				DATABASE_USER_NAME = args[++i];
				break;
			case 'p':
				DATABASE_PASSWORD = args[++i];
				break;
			case 'h':
				DATABASE_HOST = args[++i];
				break;
			default:
				System.out.println("unknown argument " + args[i]);
				return false;
			}
		}
		return true;
	}

	public static void usage(){
		System.out.println("usage: ReplicationUtil");
		System.out.println(" -c : CSV collection string (prefix with ! to exclude)");
		System.out.println(" -h : source database host[:port]");
		System.out.println(" [-u : source database username]");
		System.out.println(" [-p : source database password]");
		System.out.println(" -H : target database host[:port]");
		System.out.println(" [-U : target database username]");
		System.out.println(" [-P : target database password]");
	}
}
