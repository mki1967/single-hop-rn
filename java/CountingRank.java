/**
 *
 *    Naive simulation of counting-sort in
 *    the single-hop radio network.
 *
 *    Copyright (C) 2006  Marcin Kik
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *
 *    The author can be reached at kik@im.pwr.wroc.pl
 *
 */
/**
 *
 * This program is a naive simulation of the
 * algorithms
 * described in the paper:
 *
 *    M.Gebala, M.Kik "Counting sort and Routing in a Single Hop Radio Network."
 *    ALGOSENSORS 2007
 *
 * Note that it is sequential (not optimized) simulation and may be very slow
 * for large input size.
 *
 */


import java.util.*;

public class CountingRank{

    
    public static void main(String[] args)
    {
	int p=100, m=16, max_s=10;
	
	try{
	    if( (args[0].compareTo("-r") == 0) ||(args[0].compareTo("-a") == 0)  )
		{
		    p= Integer.parseInt(args[1]);
		    max_s= Integer.parseInt(args[2]);
		}
	    else if( args[0].compareTo("-c") == 0 )
		{
		    p= Integer.parseInt(args[1]);
		    m= Integer.parseInt(args[2]);
		    max_s= Integer.parseInt(args[3]);
		}
	    else
		{
		    p= Integer.parseInt(args[0]);
		    m= Integer.parseInt(args[1]);
		    max_s= Integer.parseInt(args[2]);
		}
	    
        } catch(Exception e) {
            System.out.println("PROGRAM SHOULD BE STARTED WITH  PARAMETERS, AS FOLLOWS:");
            System.out.println("   TO SIMULATE COUNTING RANK:");
            System.out.println("     java CountingRank $p$ $m$ $max_s$");
            System.out.println("       $p$ -- number of stations");
            System.out.println("       $m$ -- number of bits in each key");
            System.out.println("       $max_s$ -- maximal number of keys in single station");
            System.out.println("   TO SIMULATE COMPRESSED COUNTING RANK:");
            System.out.println("     java CountingRank -c $p$ $m$ $max_s$");
            System.out.println("       $p$ -- number of stations");
            System.out.println("       $m$ -- number of bits in each key");
            System.out.println("       $max_s$ -- maximal number of keys in single station");
            System.out.println("   TO SIMULATE ROUTING:");
            System.out.println("     java CountingRank -r $p$ $max_s$");
            System.out.println("       $p$ -- number of stations");
            System.out.println("       $max_s$ -- maximal number of keys (packets) in single station");
            System.out.println("   TO SIMULATE ACCELERATED ROUTING:");
            System.out.println("     java CountingRank -a $p$ $max_s$");
            System.out.println("       $p$ -- number of stations");
            System.out.println("       $max_s$ -- maximal number of keys (packets) in single station");
            System.out.println("\nTHE PROGRAM OUTPUTS RESULTS OF THE SIMULATION \n"+
                               "COMPARED WITH THE THEORETICAL UPPER BOUNDS ON THE TIME AND ENERGETIC COST.\n"+
			       "(THE ENERGETIC COST OF LISTENING (LE) AND SENDING (SE) ARE DISPLAYED SEPARATELY.)");
            System.exit(-1);
        }

	    
	

	try{
	    if((args[0].compareTo("-r") == 0) || (args[0].compareTo("-a") == 0) ){
		RadioNetwork rn=new RadioNetwork(p, Key.log2(p));
		System.out.println ("Key.log2("+p+") = "+ Key.log2(p));
	    
		rn.generateInput(max_s, true);
		System.out.println("rn.m ="+rn.m);
	        System.out.println("rn.n() = "+rn.n());
	        System.out.println("rn.r() = "+rn.r());
		System.out.println("rn.a.length ="+rn.a.length);
		System.out.println("rn.maxS() ="+rn.maxS());
		System.out.println("rn.maxR() ="+rn.maxR());
	  
		for(int i=0; i<rn.a.length; i++) rn.a[i].prepare();

		System.out.println("...");
		if(args[0].compareTo("-r") == 0) rn.RoutePackets(rn.a);
		else rn.AcceleratedRouting(rn.a);

		rn.testAfterRouting(rn.a);	
		if(args[0].compareTo("-r") == 0)
		    {    
			System.out.println("rn.clock ="+rn.clock+"  rn.RoutePacketsTime() = "+rn.RoutePacketsTime());
			System.out.println("rn.maxSE() ="+rn.maxSE()+"  rn.RoutePacketsSE() = "+rn.RoutePacketsSE());
			System.out.println("rn.maxLE() ="+rn.maxLE()+"  rn.RoutePacketsLE() = "+rn.RoutePacketsLE());
		    }
		else
		    {    
			System.out.println("rn.clock ="+rn.clock+"  rn.AcceleratedRoutingTime() = "+rn.AcceleratedRoutingTime());
			System.out.println("rn.maxSE() ="+rn.maxSE()+"  rn.AcceleratedRoutingSE() = "+rn.AcceleratedRoutingSE());
			System.out.println("rn.maxLE() ="+rn.maxLE()+"  rn.AcceleratedRoutingLE() = "+rn.AcceleratedRoutingLE());
		    }

	    }
	    else {
		RadioNetwork rn=new RadioNetwork(p, m);	    
		rn.generateInput(max_s, false);
		System.out.println("rn.m ="+rn.m);
	        System.out.println("rn.n() = "+rn.n());
	        System.out.println("rn.r() = "+rn.r());
		System.out.println("rn.a.length ="+rn.a.length);
		System.out.println("rn.maxS() ="+rn.maxS());
		System.out.println("rn.maxR() ="+rn.maxR());

		for(int i=0; i<rn.a.length; i++) rn.a[i].prepare();

		System.out.println("...");
		if(args[0].compareTo("-c") == 0 ) rn.CompressedCountingRank(rn.a, rn.m);
		else rn.CountingRank(rn.a, rn.m);

		rn.testCountingRank(rn.a);	    
		if(args[0].compareTo("-c") == 0 )
		    {
			System.out.println("rn.clock ="+rn.clock+"  rn.CompressedCountingRankTime() = "+rn.CompressedCountingRankTime());
			System.out.println("rn.maxSE() ="+rn.maxSE()+"  rn.CompressedCountingRankSE() = "+rn.CompressedCountingRankSE());
			System.out.println("rn.maxLE() ="+rn.maxLE()+"  rn.CompressedCountingRankLE() = "+rn.CompressedCountingRankLE());
		    }
		else
		    {
			System.out.println("rn.clock ="+rn.clock+"  rn.CountingRankTime() = "+rn.CountingRankTime());
			System.out.println("rn.maxSE() ="+rn.maxSE()+"  rn.CountingRankSE() = "+rn.CountingRankSE());
			System.out.println("rn.maxLE() ="+rn.maxLE()+"  rn.CountingRankLE() = "+rn.CountingRankLE());
		    }
	    }
	}
	catch(Exception e)
	    {
		System.out.println(e);
	    }
    }
};







class Key{

    public static int bit(int i, long key)
    {
	return (int) ((key>>i)&1L);
    }


    public static long mask(int m)
    {
	long mask=0, bit=1;
        int i;
        for(i=0; i<m; i++) mask=mask|bit<<i;
        return mask;
    }


    public static int log2(long x)
	// return minimal l such that 2^l >= x
    {
	int l=0;
	int y=1; // 2^0
	while(y<x) 
	    { 
		y=y+y;
		l++;
	    }

	return l;
    }

}


class Channel{
    public static final long NIL=-1;
    public static final long COLLISION=-2;

    public long message;


    public Channel() {
        clean();
    }

    void clean() {
        message=NIL;
    }

    void broadcast(long m) 
	throws Exception
    {
        if(message == NIL)
            message=m;
        else
            // message= LongSort.COLLISION;
	    throw new Exception("COLLISION -- messages: "+ message +" with "+ m);

    }

}









class Station{

    // INPUT AND OUTPUT VARIABLES
    long[] key; // table of keys stored in the station (s_i = a[i].key.length)
    int[] rank; // global rank
    boolean[] last;
    boolean[] first;
    int n; // total number of keys (to be computed in the Init procedure)

    // AUXILIARY REPLACEMENT VARIABLES USED IN ACCELERATED VERSIONS
    long[] key1;
    int[] rank1; 
    boolean[] last1;
    boolean[] first1;
    int n1;      


    int[]  gs;    // size of current group
    int[]  bg;    // number of elements ranked before current group
    int[]  rig;   // rank in current group
    int[]  rng;   // rank in next group


    int i1,i2; // interval for routing packets to this station
    long[] receivedPacket; // packets received in routing (table of size i2-i1+1)
    int packetsReceived; // packets received so far 


    

    long lrm; // copy of last received message 
    // ...    


    long sent;     //  message sent in the last time slot
    long received; //  message received in the last time slot

    // Variables for statistics

    int le; // energy for listening
    int se; // energy for sending




    void listen(Channel c) {
        received= c.message;
        le++;
    }

    void send(Channel c, long message) 
    throws Exception
    {
        c.broadcast(message);
        se++;
	sent= message;
    }



    // FOR SET S(a_i,t) = { j | bg[a_i][j]<= t < bg[a_i][j]+gs[a_i][j] }
 
    boolean inS(int t, int  j)
	// is j in S(a_i, t), where a_i denotes this station ?
    {
	return (bg[j] <= t) && (t< bg[j]+gs[j]); 
    }


    int sizeS(int t)
	// size of S(a_i,t),  where a_i denotes this station
    {
	int s=0;
	for (int j=0; j<key.length; j++)
	    if( inS(t,j) ) s++;
	return s;
    }

    // FOR SET P(a_i,k) = { j | key[a_i][j] = k }

    int minP(long k)
    {
	int min;
	for(min=0; (min < key.length) && (key[min] != k); min++);

	if(min>=key.length) System.out.println("minP: WARNING !!!"); ///

	return min;	
    }

    int maxP(long k)
    {
	int max;
	for(max=key.length-1; (max >= 0) && (key[max] != k); max--);

	if(max<0) System.out.println("maxP: WARNING !!!"); ///

	return max; 
    }

    int sizeP(long k)
    {
	int s=0;
	for(int i=0; i<key.length; i++)
	    if(key[i]== k) s++;
	return s;
    }


    boolean caseB(int t)
	// exists j such that bg[a_rcv][j]<= t = bg[a_rcv][j]+gs[a_rcv][j]-1, 
	// where a_rcv denotes this station ?
    {
	for(int j=0; j<key.length; j++)
	    if( (bg[j] <= t) && (t == bg[j]+gs[j]-1) ) return true;
	return false;
    } 


    public boolean sorted()
    {
	boolean v=true;
	for(int i=0; v && i<key.length-1; i++) v = (key[i] <= key[i+1]);
	return v;
    }

    public void internalSort()
    {
	// sorts the keys inside the station
        // we use radix sort with 8-bit digits
        int[] rank=  new int[key.length];
        int[] count= new int[256];
        long[] key1  = new long[key.length];

        int digitMask=255;

        for(int d=0; d<64; d+=8)
	    {
		for(int i=0; i<count.length; i++) count[i]=0;
		for(int i=0; i<key.length; i++) count[(int)(key[i]>>d) & digitMask ]++;
                for(int i=1; i<count.length; i++) count[i]+= count[i-1];

                for(int i=key.length-1; i>=0; i--) 
		    {
                        int idx= (int)(key[i]>>d) & digitMask ;
			rank[i]=count[idx]-1; // ranks start from zero
			count[idx]--;
		    }

		for(int i=0; i<key.length; i++) key1[rank[i]]=key[i];
		long[] tmp=key;
                key=key1;
                key1=tmp;
	    }

    }

    public void prepare()
    {
	// internalSort(); // this is done in RadioNetwork.reGenerateInput()

	gs= new int[key.length];    // size of current group
	bg= new int[key.length];    // number of elements ranked before current group
	rig= new int[key.length];   // rank in current group
	rng= new int[key.length];   // rank in next group
        rank= new int[key.length]; 
	first= new boolean[key.length];
	last= new boolean[key.length];
        sent=received= Channel.NIL;
    }

    


    int r()
	// number of distinct values in table key (we assume that key is sorted)
    {
	if(key.length==0) return 0;
	int x=1;
	for(int i=0; i<key.length-1; i++)
	    if(key[i]!= key[i+1]) x++;
	return x;
    }


    // FOR COMPRESSED RANKING

    public void swapTables()
    {
	{
	    long[] tmp= key;
	    key=key1;
	    key1=tmp;
	}
	{
	    int[] tmp= rank;
	    rank= rank1;
	    rank1=tmp;
	}
	{
	    boolean[] tmp= first;
	    first= first1;
	    first1=tmp;
	}
	{
	    boolean[] tmp= last;
	    last= last1;
	    last1=tmp;
	}
	{ 
	    int tmp =n;
	    n=n1;
	    n1=tmp;
	}
    }

    public void prepareKey1()
    {
	key1= new long[r()];
	n1=key1.length;
	if(key1.length==0) return;
        
        key1[0]=key[0];
	int i1=0;
	for(int i=1; i<key.length; i++)
	    if(key[i] != key1[i1]) 
		{
		    i1++;
		    key1[i1]=key[i];
		}
	    
    }


};



class RadioNetwork{

    int clock;  // global clock -- stations are synchronised
    Channel channel; // single-channel network 

    long seed; // used by reGenerateInput 
    int  maxK; // used by reGenerateInput 
    boolean routing; // used by reGenerateInput 

    void nextSlot()
	// tick of the clock 
    {
	clock++;
	channel.clean();
	for(int i=0; i<a.length; i++) 
	    {
		a[i].sent= Channel.NIL;
		a[i].received= Channel.NIL;
	    }
    }


    int m; // maximal length of binary representation of the key, m<64
    long mask; // computed mask with m lower bits set to one

    Station[]  a;

    // int n; // total number of keys 

    public long checkSum()
    {
	long sum=0;
	for(int i=0; i<a.length; i++)
	    for(int j=0; j<a[i].key.length; j++)
		sum+=a[i].key[j];
	return sum;
    }


    public boolean sorted()
    {
	boolean v = a[0].sorted();
	for(int i=0; v && i<a.length-1; i++) 
	    v= ( a[i].key[ a[i].key.length-1 ] <= a[i+1].key[0] ) && a[i+1].sorted();
	return v;
    }

      void generateInput(int maxK, boolean routing)
	// Generates random sequence of keys scatered among the stations.
	// creates new squence of stations.
	// each station stores at most maxK keys.
    {
        seed=System.currentTimeMillis();
	this.maxK=maxK;
	this.routing=routing;
        reGenerateInput();
    }  

    void reGenerateInput()
	// Generates random sequence of keys scatered among the stations.
	// creates new squence of stations.
	// each station stores at most maxK keys.
    {

        Random random=new Random(seed);

        System.out.println("Seed ="+seed);
        

        for(int i=0; i<a.length; i++) a[i].key=new long[random.nextInt(maxK+1)];
       
        int n=0;
	for(int i=0; i<a.length; i++) 
	    for(int j=0; j<a[i].key.length; j++){
		if(routing) a[i].key[j]= (random.nextInt(a.length));
		else a[i].key[j]= (random.nextLong() & mask);
	        n++;
	    }

	for(int i=0; i<a.length; i++) a[i].internalSort();

    }


    public RadioNetwork(int stations, int bitsInKey)
	throws Exception
    {
	if(bitsInKey>63) throw new Exception("Too many bits in key: "+bitsInKey);
        m=bitsInKey;
        mask= Key.mask(bitsInKey);

        channel = new Channel();
      
        a=new Station[stations];
        for(int i=0; i<a.length; i++) a[i]= new Station();
        
    }


    // PROCEDURES DESCRIBED IN THE TECHNCAL REPORT

    public void Init(Station[] a, int m)
	throws Exception
    {
	// all stations do internally:
        for(int i=0; i<a.length; i++) 
	    for(int j=0; j<a[i].key.length; j++) {
		//		a[i].g[j]=0; a[i].l[j]=m; // group (0,m)
                a[i].bg[j]=0; // the group (0,m) contains all the keys 
	    }
	    

	// a[0] does internally:
	a[0].lrm=0;
        for(int j=0; j<a[0].key.length; j++) a[0].rig[j]=j;
        
        // 
        for(int i=0; i<=a.length -2; i++)
	    {
		// sending
		a[i].send(channel, a[i].lrm+a[i].key.length);
		// listening
		a[i+1].listen(channel);
		// a[i+1] does internally:
		a[i+1].lrm=a[i+1].received;
		for(int j=0; j<a[i+1].key.length; j++) a[i+1].rig[j]=j+ (int) a[i+1].received;
		nextSlot();
            }

      
	// sending
        a[a.length-1].received = a[a.length-1].lrm+a[a.length-1].key.length; // 'received' denotes 'x' 
	a[a.length-1].send(channel, a[a.length-1].received);
	// listening
	for(int i=0; i< a.length-1; i++) a[i].listen(channel);
	// all stations do internally:
        for(int i=0; i<a.length; i++) 
	    {
		a[i].n=(int) a[i].received;
		for(int j=0; j<a[i].key.length; j++){
		    a[i].gs[j]= (int) a[i].received;
		}
	    }
	nextSlot();
	for(int i=0; i< a.length; i++)
	    { 
		if( (a[i].key.length>0) && (a[i].rig[0]==0) ) a[i].rng[0]=0; // it remains untouched, should be zero 
		for(int j=0; j< a[i].key.length; j++)
		    { 
			a[i].rank[j]=a[i].bg[j]+a[i].rig[j];
		    }
	    }
    }





    public void CountingRank(Station[] a, int m)
	throws Exception
    {

	System.out.print("Start Init. ");
	Init( a, m);
	System.out.println("End.");

	// testAfterInit( a, m);


	// REGROUPING PHASE

        System.out.println("Start Counting-rank.");
	for(int l=m-1; l>=0; l--) {
	    System.out.print("Start level "+l+". ");
	    for(int t=0; t<a[0].n; t++) {
		{  // FIND AND CHECK PAIR (a_snd, j')
		    int snd= -1, j1= -1;
		    for(int i=0; i< a.length; i++)
			for(int j=0; j< a[i].key.length; j++)
			    if( a[i].rank[j] == t )
				if( snd == -1 )
				    {
					snd=i;
					j1=j;
				    } 
				else
				    throw new Exception("CountingRank 1: a["+snd+"].rank["+j1+"] = "+a[snd].rank[j1]+
							"and a["+i+"].rank["+j+"] = "+a[i].rank[j]);
		    // a_snd DOES:
		    if(a[snd].rig[j1] == 0) a[snd].lrm = 0;
		    int x = (int) a[snd].lrm + ( 1- Key.bit(l, a[snd].key[j1]) );
		    if( (a[snd].sizeS(t) < a[snd].gs[j1]) &&
			( 
			 ( (a[snd].rig[j1] == a[snd].gs[j1]-1) )
			 ||
			 ( (j1== a[snd].key.length-1) || (a[snd].bg[j1+1] != a[snd].bg[j1]) ) 
			 )
			)
			a[snd].send(channel, x);
		    a[snd].received = x; // a_snd does not have to listen to know x
		}

		boolean caseA = false;
		{ // FIND AND CHECK PAIR (a_rcv, j2) for CASE A
		    int rcv=-1, j2=-1;
		    for(int i=0; i< a.length; i++)
			for(int j=0; j< a[i].key.length; j++)
			    if( 
			       (a[i].rank[j] == t+1) &&
			       ( (a[i].bg[j]<= t) && (t< a[i].bg[j]+a[i].gs[j]) )  
			       )
				if( rcv == -1 )
				    {
					rcv=i;
					j2=j;
				    } 
				else
				    throw new Exception("CountingRank 2: a["+rcv+"].rank["+j2+"] = "+a[rcv].rank[j2]+
							"and a["+i+"].rank["+j+"] = "+a[i].rank[j]);
		    if(rcv != -1)
			{
			    caseA = true;
			    // a_rcv DOES: (* CASE A *)
			    if( a[rcv].received == Channel.NIL ) a[rcv].listen(channel); // listens unless a_rcv=a_snd
			    // variable recevied contains x
			    if( Key.bit(l, a[rcv].key[j2])== 0 ) a[rcv].rng[j2]= (int) a[rcv].received;
			    else a[rcv].rng[j2]= a[rcv].rig[j2]- (int) a[rcv].received;
                            a[rcv].lrm =  a[rcv].received;
			}
		}

		{  // TEST AND COMPUTE FOR CASE B
		    for(int rcv=0; rcv< a.length; rcv++)
			if( a[rcv].caseB(t) )
			    {
				if(caseA) throw new Exception("CountingRank 3: can not be CASE A and CASE B !!!");
				// a_rcv DOES: (* CASE B *)
				if( a[rcv].received == Channel.NIL ) a[rcv].listen(channel); // listens unless a_rcv=a_snd
				for(int j=0; j<a[rcv].key.length; j++)
				    if( a[rcv].inS(t, j) )
					{
					    if( Key.bit(l, a[rcv].key[j])==0 )
						a[rcv].gs[j]= (int) a[rcv].received;
					    else
						{
						    a[rcv].bg[j]= a[rcv].bg[j]+ (int) a[rcv].received;
						    a[rcv].gs[j]= a[rcv].gs[j]- (int) a[rcv].received;	 
						}
					    a[rcv].rig[j] = a[rcv].rng[j];
					    a[rcv].rank[j] = a[rcv].bg[j]+a[rcv].rig[j];
					}
			    }
		}
		nextSlot(); // END OF TIME SLOT (l,t) OF REGROUPING PHASE
	    } // END OF  for(int t ...) 		
	    //  printState(a);
	    System.out.println("End.");
	} // END OF for(int l ...)  
	for(int i=0; i<a.length; i++)
	    for(int j=0; j<a[i].key.length; j++)
		{
		    if(a[i].rig[j] == 0)  a[i].first[j]= true;
		    else a[i].first[j]= false;
		    if(a[i].rig[j] == a[i].gs[j]-1)  a[i].last[j]= true;
		    else a[i].last[j]= false;
		}
	System.out.println("End Counting-ranks.");
    }// END OF CountingRank( ... ) 



    public void ComputeIntervals(Station[] a)
	throws Exception
    {
        System.out.print("Start Compute-intervals. ");
	for(int i=0; i<a.length; i++)
	    {
		// SLOT 2*i
		for(int snd=0; snd<a.length; snd++)
		    for(int j=0; j<a[snd].key.length; j++)
			if( (a[snd].key[j]==i) && (a[snd].first[j])) a[snd].send(channel, a[snd].rank[j]);

		a[i].listen(channel);
		if(a[i].received == Channel.NIL) {
		    a[i].i1= -1;
		    a[i].i2=-1;
		}
		else a[i].i1 = (int) a[i].received;
		nextSlot();

		// SLOT 2*i+1
		for(int snd=0; snd<a.length; snd++)
		    for(int j=0; j<a[snd].key.length; j++)
			if( (a[snd].key[j]==i) && (a[snd].last[j]) ) a[snd].send(channel, a[snd].rank[j]);

		if(a[i].i1 != -1)
		    {
			a[i].listen(channel);
			if(a[i].received<0) throw new Exception("ComputeIntervals 1: a["+i+"].received = "+a[i].received);
			a[i].i2 = (int) a[i].received;
		    }
		nextSlot();

	    }

	// prepare tables for reception of packets
	for(int i=0; i<a.length; i++) 
	    {
		a[i].packetsReceived=0;
		if(a[i].i1 != -1) a[i].receivedPacket= new long[ a[i].i2-a[i].i1+1 ];
	    }

        System.out.println("End.");
    } // END OF ComputeIntervals




    public void FinishRouting(Station[] a)
	throws Exception
    {
        System.out.print("Start Finish-routing. ");
	for(int i=0; i< a[0].n; i++)
	    {
		// SLOT i
		for(int snd=0; snd<a.length; snd++)
		    for(int j=0; j<a[snd].key.length; j++)
			if(a[snd].rank[j]== i) a[snd].send(channel, a[snd].key[j]+a.length*(snd+a.length*j)); // encode sender for tests

		for(int rcv=0; rcv<a.length; rcv++)
		    if( (a[rcv].i1<= i) && (i<=a[rcv].i2)) {
			a[rcv].listen(channel);
			if( a[rcv].received % a.length != rcv ) 
			    throw new Exception("FinishRouting: a["+rcv+"].received % a.length = "+a[rcv].received % a.length);
			a[rcv].receivedPacket[a[rcv].packetsReceived]= a[rcv].received;
			a[rcv].packetsReceived++;
		    }
		nextSlot();
	    } 
        System.out.println("End.");
    } // END OF FinishRouting


    public void RoutePackets(Station[] a)
	throws Exception
    {
	CountingRank(a, Key.log2(a.length));
	ComputeIntervals(a);
	FinishRouting(a);
    } // END OF RoutePackets


    public void AcceleratedRouting(Station[] a)
	throws Exception
    {
	System.out.println("AcceleratedRouting:");
	CompressedCountingRank(a, Key.log2(a.length));
	ComputeIntervals(a);
	FinishRouting(a);
    } // END OF RoutePackets



    public void CompressedCountingRank(Station[] a, int m)
	throws Exception
    {

	System.out.println("CompressedCountingRank:");

	for(int i=0; i<a.length; i++)
	    {
		a[i].prepareKey1();
		a[i].swapTables();
		a[i].prepare();
	    }
	
	CountingRank(a,m);
	
	// System.out.print("INSIDE CompressedCR: "); testCountingRank(a);

	for(int i=0; i<a.length; i++) 
	    {
		a[i].swapTables();
		a[i].prepare(); // creates tables: rank, first, last 
	    }

        ExpandRanks(a);

    }


    public void ExpandRanks(Station[] a)
	throws Exception
    {
	// RESTORE first AND last
	for(int i=0; i<a.length; i++)
	    for(int j=0; j<a[i].key.length; j++)
		{
		    a[i].first[j]= false;
		    a[i].last[j]= false;
		}

	for(int i=0; i<a.length; i++)
	    for(int j1=0; j1<a[i].key1.length; j1++)
		{
		    a[i].first[ a[i].minP( a[i].key1[j1] ) ]= a[i].first1[j1];
		    a[i].last[ a[i].maxP( a[i].key1[j1] ) ]= a[i].last1[j1];
		}

	for(int i=0; i<a.length; i++) a[i].lrm= 0; 


	for(int t=0; t< a[0].n1-1; t++)
	    {
		for(int snd=0; snd<a.length; snd++)
		    for(int j=0; j<a[snd].key1.length; j++)
			if(a[snd].rank1[j] == t)
			    {
				int p1= a[snd].minP(a[snd].key1[j]);
				int c= a[snd].sizeP(a[snd].key1[j]);
				for(int d=0; d<c; d++)
				    a[snd].rank[p1+d]= (int) a[snd].lrm+d;
				a[snd].send(channel, a[snd].lrm+c);
			    }

		for(int rcv=0; rcv<a.length; rcv++)
		    for(int j=0; j<a[rcv].key1.length; j++)
			if(a[rcv].rank1[j] == t+1) 
			    {
				a[rcv].listen(channel);
				a[rcv].lrm= a[rcv].received;
			    }

		nextSlot();
	    } // END OF for(int t= ...)

	for(int snd=0; snd<a.length; snd++)
	    for(int j=0; j<a[snd].key1.length; j++)
		if(a[snd].rank1[j] == a[0].n1-1)
		    {
			int p1= a[snd].minP(a[snd].key1[j]);
			int c= a[snd].sizeP(a[snd].key1[j]);
			for(int d=0; d<c; d++)
			    a[snd].rank[p1+d]= (int) a[snd].lrm+d;
			a[snd].send(channel, a[snd].lrm+c);
			a[snd].received=a[snd].sent;
		    }

	for(int i=0; i<a.length; i++)
	    {
		if(a[i].received == Channel.NIL ) a[i].listen(channel);
		a[i].n = (int) a[i].received;
	    } 
	
	nextSlot();	    

    }// END OF ExpandRanks



    /// THEORETICAL BOUNDS ON COMPLEXITIES

    int n()
    {
	int s=0;
	for(int i=0; i<a.length; i++) s= s+a[i].key.length;
	return s;
    }

    int r()
    {
	int s=0;
	for(int i=0; i<a.length; i++) s= s+a[i].r();
	return s;
    }





    int maxS()
    {
	int m=a[0].key.length;
	for(int i=1; i<a.length; i++) if(a[i].key.length>m) m=a[i].key.length;
	return m;
    }

    int minS()
    {
	int m=a[0].key.length;
	for(int i=1; i<a.length; i++) if(a[i].key.length<m) m=a[i].key.length;
	return m;
    }


    public int maxD()
	// should be computed only for routing
    {
	int[] d=new int[a.length];
	for(int i=0; i<a.length; i++)
	    for(int j=0; j<a[i].key.length; j++) d[(int) a[i].key[j]]++;
	int max=d[0];
	for(int i=1; i<d.length; i++) 
	    if(max<d[i]) max=d[i];
	return max;
    }  


    int maxR()
    {
	int max=a[0].r();
	for(int i=1; i<a.length; i++)
	    if(a[i].r() > max) max=a[i].r();
	return max;
    }

    int maxSE()
	// maximal sending energy
    {
	int max= a[0].se;
	for(int i=1; i<a.length; i++)
	    if(a[i].se>max) max=a[i].se;
	return max;
    }


    int maxLE()
	// maximal sending energy
    {
	int max= a[0].le;
	for(int i=1; i<a.length; i++)
	    if(a[i].le>max) max=a[i].le;
	return max;
    }





    int CountingRankTime()
    {
	// p+m*n
	return a.length+m*n();
    }

    int CountingRankSE()
    {
	// 1+m*r
	return 1+m*maxR();
    }

    int CountingRankLE()
    {
	// 2+2*m*r
	return 2+2*m*maxR();
    }

    int CompressedCountingRankTime()
    {
	// p+m*n+r
	return a.length+m*r()+r();
    }

    int CompressedCountingRankSE()
    {
	// 1+m*r
	return 1+m*maxR()+maxR();
    }

    int CompressedCountingRankLE()
    {
	// 2+2*m*r
	return 2+2*m*maxR()+maxR()+1;
    }

    int RoutePacketsTime()
    {
	// m*n+n+3*p
	return m*n()+n()+3*a.length;
    }

    int RoutePacketsSE()
    {
	//m*maxR+maxS+2*maxR+1
	return m*maxR()+maxS()+2*maxR()+1;
    }

    int RoutePacketsLE()
    {
	// 2*m*maxR + maxD + 4
	return 2*m*maxR()+ maxD() +4;
    }

    int AcceleratedRoutingTime()
    {
	// m*r+n+3*p+r
	return m*r()+n()+3*a.length+r();
    }

    int AcceleratedRoutingSE()
    {
	//m*maxR+maxS+2*maxR+1
	return m*maxR()+maxS()+3*maxR()+1;
    }

    int AcceleratedRoutingLE()
    {
	// 2*m*maxR + maxD + 4
	return 2*m*maxR()+ maxD()+maxR()+5;
    }

  

    /// PROCEDURES FOR TESTING

    public void testAfterInit(Station[] a, int m)
	throws Exception
    {
	// recompute n
        int n=0;
        for(int i=0; i<a.length; i++) n= n+a[i].key.length;        
        System.out.println("testAfterInit: n = "+n);
          
        // test a[i].n and a[i].gs[j]
	
        for(int i=0; i<a.length; i++)
	    {
		if(a[i].n!=n) throw new Exception("a["+i+"].n = "+a[i].n+"  ( !=  n = "+n+")");
		for(int j=0; j<a[i].key.length; j++)
		    if(a[i].gs[j]!=n) throw new Exception("a["+i+"].gs["+j+"] = "+a[i].gs[j]+"  ( !=  n = "+n+")");
	    }

        // test a[i].rig[j]

        int r=0;
        for(int i=0; i<a.length; i++)
	    for(int j=0; j<a[i].key.length; j++)
		{
		if(a[i].rig[j]!=r) throw new Exception("a["+i+"].rig["+j+"] = "+a[i].rig[j]+"  ( !=  r = "+r+")");
		r++;
		}

        System.out.println("testAfterInit: OK");
    }

    void testCountingRank(Station[] a)
	throws Exception
    {
	long[] tKey= new long[ a[0].n ]; 
        int[]  tI= new int[ a[0].n ];
        int[]  tJ= new int[ a[0].n ];

	for(int t=0; t< tKey.length; t++) tKey[t]= -1; // keys are non-negative

	for(int i=0; i<a.length; i++)
	    for(int j=0; j<a[i].key.length; j++)
		{
		    int r= a[i].rank[j];
		    if(tKey[r] != -1) throw new Exception("testCountingRank 1: a["+tI[r]+"].rank["+tJ[r]+"] = "+a[tI[r]].rank[tJ[r]]+
							     " and a["+i+"].rank["+j+"] = "+a[i].rank[j]);
		    tKey[r] = a[i].key[j];
		    tI[r] = i;
		    tJ[r] = j;
		}
	for(int r=0; r<tKey.length; r++)
	    if(tKey[r] == -1) throw new Exception("testCountingRank 2: tKey["+r+"] = "+tKey[r]);
	
	for(int r=0; r<tKey.length-1; r++)
	    if(
	       (tKey[r] > tKey[r+1]) ||
	       ((tKey[r] == tKey[r+1]) && (tI[r]> tI[r+1])) ||
	       ((tKey[r] == tKey[r+1]) && (tI[r]== tI[r+1]) && (tJ[r] >= tJ[r+1]))
	       )
		throw new Exception("testCountingRank 3: t["+r+"] = ("+tKey[r]+","+tI[r]+","+tJ[r]+")"+
				    "and t["+(r+1)+"] = ("+tKey[r+1]+","+tI[r+1]+","+tJ[r+1]+")");

	System.out.println("testCountingRank: OK");
	       

    }


    void testAfterRouting(Station[] a)
	throws Exception
      
    {
	// DESTRUCTIVE TEST (DELETES INPUT KEYS)
	// received packets have encoded sending position (snd,j) and destination
	for(int i=0; i<a.length; i++)
	    {
		if( a[i].i1 != -1){
		    if( (a[i].packetsReceived!=a[i].receivedPacket.length))
			throw new Exception("testAfterRouting 1: a["+i+"].packetsReceived = "+a[i].packetsReceived+
					    " != a[i].receivedPacket.length = "+a[i].receivedPacket.length);
		    for(int j=0; j<a[i].receivedPacket.length; j++)
			{
			    int p= a.length;
			    int rcv= (int) a[i].receivedPacket[j]%p;
			    if(rcv != i)
				throw new Exception("testAfterRouting 2: rcv = "+rcv+" != i = "+i);
			    int snd= (int) (a[i].receivedPacket[j]/p)%p;
			    int jSnd= (int) (a[i].receivedPacket[j]/p)/p;
			    if( a[snd].key[jSnd] != i )
				throw new Exception("testAfterRouting 3: a["+snd+"].key["+jSnd+"] = "+a[snd].key[jSnd]+
						    " != i = "+i);
			    a[snd].key[jSnd] = -1;  // DELETE PACET DELIVERED
			    
			}
		}
	    }

	for(int i=0; i<a.length; i++)
	    for(int j=0; j<a[i].key.length; j++)
		if(a[i].key[j]!=-1)
		    throw new Exception("testAfterRouting 4: a["+i+"].key["+j+"] = "+ a[i].key[j]+" != -1");
        
	System.out.println("testAfterRouting: OK");
	
        reGenerateInput();
    } // END OF TEST AFTER ROUTING



    void printState(Station[] a)
    {
	System.out.println("state:");
	for(int i=0; i< a.length; i++)
	    {
		System.out.println("a["+i+"]");
		for(int j=0; j<a[i].key.length; j++)
		    System.out.println(j+" k="+a[i].key[j]+" bg="+a[i].bg[j]+" gs="+a[i].gs[j]+" rig="+a[i].rig[j]+" r="+a[i].rank[j]);
		
	    }
    }



};

