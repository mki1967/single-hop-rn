/**
 *
 *    Simulation of the algorithm for correcting sorted sequences in
 *    a single-hop radio network.
 *
 *    Copyright (C) 2004  Marcin Kik
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
 *    The author can be reached at Marcin.Kik@pwr.wroc.pl
 *
 */
/**
 *
 * This program is a simulation of the algorithm
 * described in the paper:
 *
 *    M.Kik "Correcting Sorted Sequences in a Single Hop Radio Network".
 *    FCT 2009
 *
 *
 * Compile the program with:
 *
 *    javac CorrectionRN.java
 *
 * Run the program with:
 *
 *    java CorrectionRN n k [seed]
 *
 * where:
 *    n - the length of the sequence (number of stations)
 *    k - number of changes
 *    seed - for random number generator used for generating the input 
 */




import java.util.*;

public class CorrectionRN{

    static final int NIL=-1;

    Timer timer; // global timer
    
    Channel channel;

    int clock;


    Station[] s; // table of stations
    VirtualStation[][] vs; // table of virtual stations

    public CorrectionRN(int n)
    {
	timer = new Timer();



	channel= new Channel();
        s = new Station[n];
        for(int i=0; i<s.length; i++) 
	    s[i]=new Station();
    }


    void nextSlot()
    {
	channel.clear();
	clock++;
    }


class Station{

    int le;
    int se;
    

    // variables used by real stations only
    int oldKey;
    int newKey;
    int oldIdx;
    int idxA;
    int idxB;
    int changed;
    int sum;
    boolean last;
    int mov;

    // variables used by real and virtual stations
    int k;
    int key;
    int idx;
    int rank;
    int newIdx;
    int newRank;
    int rworker;
    int iworker;

    public Station()
    {
      
    }

    void send(Channel c, int msg) throws Exception
    {
	se++;
	c.broadcast(msg);
    }

    int listen(Channel c)
    {
	le++;
	return c.receive();
    }

};


class VirtualStation extends Station{
    Station host;

    public VirtualStation(Station creator)
    {
	host=creator;
    }

    void send(Channel c, int msg) throws Exception
    {
	host.send(c,msg);
    }

    int listen(Channel c)
    {
	return host.listen(c);
    }


    
}


class Channel{
    int message;


    public Channel()
    {
	clear();
    } 

    void clear()
    {
	message=NIL;
    }

    void broadcast(int msg) throws Exception
    {
	if(msg<0) throw new Exception("broadcasting "+msg+" < 0");
	if(message!=NIL) throw new Exception("collision !");
	message=msg;
    }

    int receive()
    {
	return message;
    }

}






// This class is used for more efficient simulation of waking up the listeners
class Timer{
    class Element{
	Station station;
	Element next;
	public Element( Station s)
	{
	    station=s;
	}
    }

    Element[] table;



    void reset(int n)
    {
	table = new Element[n];
    }

    void set(Station s, int t)
    {
	// if(t<0 || t>= table.length) return;
	Element e= new Element(s);
	e.next=table[t];
        table[t]=e;
    }


    Station pop(int t)
    {
	if(table[t]==null) return null;

	Station s= table[t].station;
        table[t]=table[t].next;
        return s;
    }
};



    // Procedures of the algorithm

    void splitAndCount() throws Exception
    {

	int n= s.length;

	for(int i=0; i<n; i++){
	    s[i].idx=s[i].oldIdx;
	    s[i].sum=0;
	    s[i].idxA= NIL;
	    s[i].idxB= NIL;
	    s[i].key=s[i].newKey;
	    if(s[i].newKey != s[i].oldKey) s[i].changed=1; else s[i].changed=0;
	}

	Station[] sender= new Station[n];
	Station[] listener=new Station[n];
	
	for(int i=0; i<n; i++){
	    sender[s[i].idx]=s[i];
	    if(s[i].idx>0) listener[s[i].idx-1]=s[i];
	}

	for(int t=0; t < n-1; t++){
	    if(sender[t].changed == 1) sender[t].idxB= sender[t].sum;
	    else sender[t].idxA= sender[t].idx-sender[t].sum;
	    sender[t].send(channel, sender[t].sum+sender[t].changed);

	    listener[t].sum= listener[t].listen(channel);

	    nextSlot();
	}

	if(sender[n-1].changed == 1) sender[n-1].idxB= sender[n-1].sum;
	else sender[n-1].idxA= sender[n-1].idx-sender[n-1].sum;
	sender[n-1].send(channel, sender[n-1].sum+sender[n-1].changed);

        for(int i=0; i < s.length; i++){
	    s[i].k= s[i].listen(channel);
	}

	nextSlot();

	//	System.out.println("s[0].k=="+s[0].k); // TEST 

    }


    void assignWorkers() throws Exception
    {
	Station[] sender= new Station[s[0].k];
	for(int i=0; i<s.length; i++)
	    if(s[i].idxB != NIL) sender[s[i].idxB]=s[i];

	int gs= s.length/s[0].k;

	vs=new VirtualStation[s[0].k][];

	for(int i=0; i<sender.length; i++)
	    if(sender[i]==null) throw new Exception("assignWorkers: sender["+i+"]== null");

	for(int t=0; t<s[0].k; t++){
	    sender[t].send(channel, sender[t].newKey);
            vs[t]= new VirtualStation[gs];
	    for(int j=t*gs; j<(t+1)*gs; j++){
		int t1= j % gs;
		vs[t][t1]=new VirtualStation(s[j]);
		vs[t][t1].key= vs[t][t1].listen(channel);
		vs[t][t1].rworker=0;
		vs[t][t1].iworker=gs-1;
	    }
	    
	    nextSlot();
	}
    }




    void transferRanks(int i1, int i2) throws Exception
    {

	int gs= s.length/s[0].k;

	for(int t=0; t<= i2-i1; t++){
	    int i=i1+t;
	    int rw=vs[i][0].rworker;
	    int rw1= (rw+1)%gs;
	    vs[i][rw].send(channel, vs[i][rw].newRank);
            vs[i][rw1].rank=vs[i][rw1].listen(channel);

	    nextSlot();
	} 


	for(int i=i1; i<= i2; i++)
	    for(int j=0; j<gs; j++)
		vs[i][j].rworker= (vs[i][j].rworker+1) % gs;
    }

    void sendRanksToIndexes(int i1, int i2, Station[] bFM) throws Exception
    // bFM - stations b[0], ..., b[k-1] in finalMerge() 
   {
	

	int gs= s.length/s[0].k;

	for(int t=0; t<= i2-i1; t++){
	    int i=i1+t;
	    int iw=vs[i][0].iworker;
	    int rw=vs[i][0].rworker;
	    vs[i][rw].send(channel, vs[i][rw].newRank);
            vs[i][iw].newIdx=vs[i][iw].idx + vs[i][iw].listen(channel);

	    if(bFM != null) bFM[t].rank=bFM[t].listen(channel);

	    nextSlot();
	} 


	for(int i=i1; i<= i2; i++)
	    for(int j=0; j<gs; j++)
		vs[i][j].rworker= (vs[i][j].rworker+1) % gs;
    }



    void rank(int i1, int i2, Station[] b, int d, Station[] bFM) throws Exception
    {

	int m=b.length;

	Station[] sender= new Station[m];


	for(int i=0; i<m; i++)
	    {
		int t=BSO.bso(m, b[i].idx);
		if(sender[t]!=null) 
		    throw new Exception("rank: collision of senders!");
		sender[t]=b[i];
	    }


	for(int i=i1; i<=i2; i++)
	    {
		int rw=vs[i][0].rworker;
		vs[i][rw].rank=0;
	    }



	for(int l=0; l<= BSO.h(m)-2; l++)
	    {
		Station[] a = new Station[i2-i1+1];
		for(int i=i1; i<=i2; i++)
		    {
			int rw=vs[i][0].rworker;
			a[i-i1]=vs[i][rw];
		    }
		lrank(l, a, sender, d);
		transferRanks(i1,i2);
	    }



	Station[] a = new Station[i2-i1+1];
	for(int i=i1; i<=i2; i++)
	    {
		int rw=vs[i][0].rworker;
		a[i-i1]=vs[i][rw];
	    }


	lrank(BSO.h(m)-1, a, sender, d);


	sendRanksToIndexes(i1,i2, bFM);
    }


    void lrank(int l, Station[] a, Station[] b, int d) throws Exception
    {

	// In the simulation, b is sorted by bso(b[i].idx)
	int m=b.length;
	int levsize=BSO.levsize(m,l);
	Station[] sender=new Station[levsize];
        int y1=BSO.at(l,0);



	int fullsize=BSO.pow2(l);
	timer.reset(fullsize);


	for(int i=0; i<a.length; i++)
	    timer.set(a[i], a[i].rank);



	for(int r=0; r<levsize; r++)
	    {
		if(BSO.at(l,r)!=  BSO.bso(m,b[y1+r].idx)) 
		    throw new Exception("lrank: bad sender!");
		b[y1+r].send(channel, b[y1+r].key);
		//		System.out.println("l="+l+"; b[y1+r].idx="+b[y1+r].idx);

		Station a1;
		while((a1=timer.pop(r))!= null)
		    {
			int msg= a1.listen(channel);
			if((a1.key <= msg && d==0) || (a1.key<msg && d==1))
			    a1.newRank=2*a1.rank;
			else
			    a1.newRank=2*a1.rank + 1;
		    }

		nextSlot();

	    }


	for(int i=levsize; i<fullsize; i++)
	    {
		Station a1;
		while((a1=timer.pop(i))!= null)
		    a1.newRank=a1.rank+levsize;

	    }
	    

    }  


    void transferIndexes(int i1, int i2) throws Exception
    {

	int gs= s.length/s[0].k;

	for(int t=0; t<= i2-i1; t++){
	    int i=i1+t;
	    int iw=vs[i][0].iworker;
	    int iw1= (iw+gs-1)%gs;
	    vs[i][iw].send(channel, vs[i][iw].newIdx);
            vs[i][iw1].idx=vs[i][iw1].listen(channel);

	    nextSlot();
	} 


	for(int i=i1; i<= i2; i++)
	    for(int j=0; j<gs; j++)
		vs[i][j].iworker= (vs[i][j].iworker+gs-1) % gs;
    }


    void merge(int i1, int i2, int i3, int i4) throws Exception
    {

	//	System.out.println("merge("+i1+","+i2+","+i3+","+i4+") ...");
	
	Station[] b34=new Station[i4-i3+1];
	for(int i=i3; i<= i4; i++) 
	    {
		int iw=vs[i][0].iworker;
		b34[i-i3] = vs[i][iw];
	    }
	rank(i1,i2,b34, 0, null);

	Station[] b12=new Station[i2-i1+1];
	for(int i=i1; i<= i2; i++) 
	    {
		int iw=vs[i][0].iworker;
		b12[i-i1] = vs[i][iw];
	    }
	rank(i3,i4,b12, 1, null);

	transferIndexes(i1,i2);
	transferIndexes(i3,i4);

    }


    void sort() throws Exception
    {
	int k=s[0].k;

	for(int i=0; i<k; i++)
	    {
		int iw=vs[i][0].iworker;
		vs[i][iw].idx=0;
	    }
	int m=1; 
	while(m<k)
	    {
		//		System.out.println("sort: m="+m);
		for(int i=0; i< k/(2*m); i++)
		    merge(2*i*m, (2*i+1)*m-1, (2*i+1)*m, (2*i+2)*m-1);

		if( k%(2*m) > m){
		    int i1=(k/(2*m))*2*m;
		    merge(i1,i1+m-1, i1+m, k-1);
		} 

		m=2*m;
	    }
    }


    void finalMerge() throws Exception
    {
	int n=s.length;
	int k=s[0].k;

	Station[] a=new Station[n-k];
	Station[] b=new Station[k];


	for(int i=0; i<n; i++)
	    if(s[i].idxA != NIL) 
		{
		    if(a[s[i].idxA]!= null) 
			throw new Exception("finalMerge: collision in a");
		    a[s[i].idxA]=s[i];
                    s[i].idx=s[i].idxA;
		}
	    else
		{
		    if(b[s[i].idxB]!= null) 
			throw new Exception("finalMerge: collision in b");
		    b[s[i].idxB]=s[i];
		}

	for(int i=0; i<n-k-1; i++)
	    if(a[i].key>a[i+1].key) throw new Exception("finalMerge: a NOT SORTED"); 

	//	for(int i=0; i<n-k; i++) System.out.println("a["+i+"].key="+a[i].key); 


	rank(0, k-1, a, 0, b);

	for(int t=0; t<k; t++)
	    {
		Station v=vs[t][ vs[t][0].iworker ];
		v.send(channel, v.newIdx);
		int msg=b[t].listen(channel);
		b[t].newIdx=msg;
                b[t].idx=msg-b[t].rank;

		nextSlot();
	    }

	Station[] b1=new Station[k]; // b sorted by idx
	for(int i=0; i<k; i++)  b1[b[i].idx]=b[i];

	for(int i=0; i<k-1; i++)
	    if(b1[i].key>b1[i+1].key) throw new Exception("finalMerge: b1 NOT SORTED"); 



	b1[k-1].last=true;

	for(int t=k-1; t>0; t--)
	    {
		b1[t].send(channel, b1[t].rank);
		int msg= b1[t-1].listen(channel);
		if(b1[t-1].rank != msg) b1[t-1].last=true;
		else b1[t-1].last= false;

		nextSlot(); 
	    }

	/*
	for(int i=0; i<k; i++) 
	    System.out.println("b1["+i+"]: key ="+b1[i].key+
			       "; rank="+b1[i].rank+
			       "; last="+b1[i].last);
	*/

	Station[] sender=new Station[n-k+1];

	for(int i=0; i<k; i++)
	    if(b[i].last)
		{
		    if(sender[b[i].rank]!=null)
			throw new Exception("final-merge: collision in sender");
		    sender[b[i].rank]=b[i];
		}
	    
	a[0].mov=0;
	for(int i=1; i<n-k; i++)  a[i].mov=NIL;


	for(int t=0; t<n-k; t++)
	    {
		if(sender[t]!=null)
		    sender[t].send(channel, sender[t].idx);
		int msg= a[t].listen(channel);
		if(msg!=NIL) a[t].mov= msg+1; 

		nextSlot();
	    }

	for(int t=0; t<n-k-1; t++)
	    {
		a[t].send(channel, a[t].mov);
		if(a[t+1].mov==NIL)
		    {
			a[t+1].mov= a[t+1].listen(channel);
		    }

		nextSlot();
	    }

	for(int i=0; i<n-k; i++) a[i].newIdx=a[i].idx+a[i].mov;


    }



    void correction() throws Exception
    {
	System.out.println("Starting correction.");
	System.out.println("-> splitAndCount ...");
	splitAndCount();
	if(s[0].k==s.length)
	    throw new Exception("n=k="+s[0].k+" APPLY STANDARD SORTING\n");
	if(s[0].k>0)
	    {
		System.out.println("-> assignWorkers ...");
		assignWorkers();
		System.out.println("-> sort ...");
		sort();
		System.out.println("-> finalMerge ...");
		finalMerge();
		for(int i=0; i<s.length; i++)
		    {
			s[i].oldIdx=s[i].newIdx;
			s[i].oldKey=s[i].newKey;
		    }
	    }
	System.out.println("correction finished.");
    }




    // Auxiliary procedures

    void generateInput(long seed, int k) throws Exception
    {

	int n=s.length;

	if(k>n) 
	    throw new Exception("k = "+k+" > n = "+n);
	Random random=new Random(seed);


	Station[] s= new Station[n];
	{
	    // Initial indexes are randomly permuted
	    int[] idx= new int[n];
	    for(int i=0; i<n; i++) idx[i]=i;
	    for(int i=0; i<n; i++)
		{
		    int x=random.nextInt(n-i);
		    int tmp= idx[x];
		    idx[x]= idx[n-i-1];
		    idx[n-i-1]=tmp;
		}

	    // for(int i=0; i<n; i++) System.out.println(idx[i]); // TEST

	    for(int i=0; i<n; i++) this.s[i].oldIdx=idx[i];
	    for(int i=0; i<n; i++) s[this.s[i].oldIdx]=this.s[i];
	}

	int[] old= new int[n];
	for(int i=0; i<n; i++) old[i]= random.nextInt(Integer.MAX_VALUE);

        Arrays.sort(old);


	for(int i=0; i<n; i++) 
	    s[i].oldKey=s[i].newKey= old[i];



	int[] idx= new int[n];
	for(int i=0; i<n; i++) idx[i]=i; 
	for(int i=0; i<k; i++)
	    {
		int x=random.nextInt(n-i);
		int tmp= idx[x];
		idx[x]= idx[n-i-1];
                idx[n-i-1]=tmp;
	    }

	for(int i=n-k; i<n; i++)
	    s[idx[i]].newKey= random.nextInt(Integer.MAX_VALUE);

	int changes=0;
	
	// TEST
	for(int i=0; i<n; i++) 
	    {
		// System.out.print("oldIdx = "+s[i].oldIdx+"  old = "+s[i].oldKey+"   new = "+s[i].newKey ); 
		if(s[i].oldKey!= s[i].newKey)
		    {
			// System.out.println(" <-");
			changes++;
		    }
		// else System.out.println();
	    }
	System.out.println("real changes k = "+changes);
	/**/

    }


    void testOutput() throws Exception
    {
	int n= s.length;
	int[] out=new int[n];

	for(int i=0; i<n; i++) out[i]=NIL;

	for(int i=0; i<n; i++)
	    {
		if(out[s[i].oldIdx]!=NIL)
		    throw new Exception("testOutput: COLLISION AT "+s[i].oldIdx);
		out[s[i].oldIdx]=s[i].newKey;
	    }


	//	for(int i=0; i<n; i++) System.out.println("out[i]="+out[i]);

	for(int i=0; i<n; i++)
	    if(out[i]==NIL)
		throw new Exception("testOutput: NIL AT "+i);

	for(int i=0; i<n-1; i++)
	    if(out[i]>out[i+1])
		throw new Exception("testOutput: NOT SORTED !!!");

	System.out.println("Output: OK");
	    
    }

    void printCosts()
    {
	int n= s.length;
	int k=s[0].k;
	int tBound=BSO.timeBound(n,k);

	System.out.println("TIME: clock =="+ clock+", upper bound ="+tBound+((clock<=tBound)?" OK": " WRONG !!!"));

	int maxLE = s[0].le;
	int maxSE = s[0].se;
	int maxE = s[0].le+s[0].se;

	for(int i=1; i<s.length; i++)
	    {
		if(s[i].le > maxLE) maxLE=s[i].le;
		if(s[i].se > maxSE) maxSE=s[i].se;
		if(s[i].le+s[i].se > maxE) maxE=s[i].le+s[i].se;
	    }
	System.out.println("ENERGETIC COSTS:");

	System.out.println("maximal Listening Energy = "+maxLE);
	System.out.println("maximal Sending Energy = "+maxSE);
	int eBound=BSO.energyBound(n,k);
	System.out.println("maximal Energy = "+maxE+", upper bound ="+eBound+((maxE<=eBound)?" OK": " WRONG !!!"));
        


    }

    public static void main(String[] args)
    {
	try
	    {
		for(int i=0; i<args.length; i++)
		    System.out.println("args["+i+"]= "+args[i]);

		if(args.length!= 3 && args.length!=2) 
		    throw new Exception(
					"Run the program with:\n"+
					"\n"+
					"    java CorrectionRN n k [seed]\n"+
					"\n"+
					"where:\n"+
					"    n - the length of the sequence (number of stations)\n"+
					"    k - number of changes\n"+
					"    seed - for random number generator used for generating the input\n"+
					"For example:\n"+
					"    java CorrectionRN 1000 100\n" 
					);

		int n=Integer.parseInt(args[0]);
		System.out.println("n = "+n);

		int k=Integer.parseInt(args[1]);
		System.out.println("k = "+k);

		long seed= args.length==3 ? Long.parseLong(args[2]): System.currentTimeMillis();
		System.out.println("seed = "+seed);

		CorrectionRN rn= new CorrectionRN(n);

		rn.generateInput(seed,k);

		rn.correction();

		rn.testOutput();

		rn.printCosts();
	    }
	catch(Exception e)
	    {
		System.out.println(e);
	    }
    }

};


/**
The class BSO contains functions useful in computations
related to Binary Search Orderings.
Binary Search Ordering (BSO) is a permutation of the set $\{0,\ldots,m-1\}$
defined as follows:
Let $T[0,\ldots,m-1]$ be a table representing binary tree of $m$ nodes.
$T[0]$ is a root of the tree and, 
for each $y\ge 0$, $T[2(y+1)-1]$ (respectively, $T[2(y+1)]$) is the left 
(respectively, the right) child of $T[y]$. 
Let the elements $0,\ldtos, m-1$ be placed in $T$ in such a way that the binary
tree represented by $T$ is ordered {\em inorder} 
(i.e. for each $T[y]$, 
all the elements in its left subtree are less than $T[y]$ and 
all the elemetns in its right subtree are greater than $T[y]$.)
There is only one such placement possible.
Then, the permutation reverse to BSO is defined as follows:
$bso^{-1}(y)=T[y]$.
The permutation BSO is defined as follows:
$bso(x)$ is the index $y$, such that $T[y]=x$.


*/

class BSO{

    public static int max(int x, int y)
    {
	if(x>y) return x;
	else return y;
    }

    public static int pow2(int x)
    /**
     $2^x$, where $x \ge 0$.
     For arbitrary $x$, returns $\max\{1,2^x\}$
     */    
    {
	int p=1;
        for(int i=0; i<x; i++) p=2*p;
	return p;
    }

    public static int clg(int x)
    /**
     $\lceil \log_2 x \rceil$, where $x \ge 1$.
     For $x \le 0$, returns $0$. 
     */   
    {
	int i=0, p=1;
        while(p<x){
	    p=2*p;
            i++;
	}
	return i;        
    }

    public static int flg(int x)
    /**
     $\lfloor \log_2 x \rfloor$, where $x \ge 1$.
     For $x \le 0$, returns $0$. 
     */   
    {
	int i=0, p=1;
        while(2*p <= x){
	    p=2*p;
            i++;
	}
	return i;        
    }


    public static int cdiv(int a, int b)
    /**
       For $a\ge 0$ and $b>0$ returns $\lceil a/b \rceil$
     */
    {
	return (a+b-1)/b;
    }

    public static int fdiv(int a,int b)
    /**
       For $a\ge 0$ and $b>0$ returns $\lfloor a/b \rfloor$
     */
    {
	return a/b;
    }




    public static int h(int m)
    /**
     height of the tree containing $m$ elements:
        $ h(m) = \lceil \log_2 (m+1) \rceil $
     For $m < 0$, returns $0$. 
     */
    {
	return clg(m+1);
    }


    public static int fs(int h)
    /**
     The size of full binary tree of height $h$, where $h\ge 0$.
     $ fs(h) = 2^h-1 $
     */
    {
	return pow2(h)-1;
    }


    public static int ml(int m)
    /**
     Missing leaves on the last level of the tree with $m$ elements:
     $ ml( m ) = fs( h(m) ) - m $
     */
    {
	return fs(h(m))-m;
    }

    public static int ls(int m)
    /**
     Size of the left subtree of the root in the tree with $m$ elements:
     if $m \le 1$ then $ ls(m) = 0$ 
     else $ ls(m) = fs(h(m)-1) - \max\{ 0, ml(m)-2^{h(m)-2} \}
     */
    {      
	if(m<=1) return 0;
	else
	    { 
		int h1=h(m);
		return fs( h1-1 ) - max(0, ml(m)-pow2(h1-2));
	    }
    }

    public static int rs(int m)
    /** 
     Size of the right subtree of the root in the tree with $m$ elements:
     $ rs(m) = m - 1 - ls(m) $
    */
    {
	return m-1-ls(m);
    }

    public static int l(int y)
    /**
     Left son of T[y] (computed as in binary heap):
     $left(y)= 2(y+1)-1$ 
     */
    {
	return 2*(y+1)-1;
    }

    public static int r(int y)
    /**
     Right son of T[y] (computed as in binary heap):
     $r(y)= 2(y+1)$ 
     */
    {
	return 2*(y+1);
    }

    public static int p(int y)
    /**
     parent of T[y] (computed as in binary heap):
     $p(y)= \lfloor y-1 / 2\rfloor$ 
     */
    {
        if(y <= 0) return y;
	return (y-1)/2;
    }


    public static int lev(int y)
    /**
     level of T[y] in the tree:
     $lev(y)=\lfloor \log_2 (y+1) \rfloor$
     */
    {
	return flg(y+1);
    }


    public static int inlev(int y)
    /**
     position of T[y] within its level:
     $inlev(y)= y - fs(lev(y))$
     */
    {
	return y-fs(lev(y));
    }


    public static int at(int l, int i)
    /**
       returns y such that T[y] is $i$th node on level $l$:
       $at(l,i)= fs(l)+i$
     */
    {
	return fs(l)+i;
    }


    public static int levsize(int m, int l)
    /**
       size of the level $l$ in the tree containing $m$ elements.
     */
    {
	if(l<0 || l> h(m)-1) return 0;
	if(l == h(m)-1) return pow2(l)-ml(m);
	return pow2(l);
    }


    // COST BOUNDS CLAIMED IN THE PAPER

    public static int energyBound(int n, int k)
    /**
       returns the upper bound on the energy
       as stated in the paper
     */
    {
	if( (clg(k)+1)*(clg(k)+2)/2+clg(k) <= fdiv(n,k) )
	    return 14;
	else
	    return 
		3*cdiv( (clg(k)+1)*(clg(k)+2), 2*fdiv(n,k) )
		+4*cdiv( clg(k), fdiv(n,k) )
		+10;
    }


    public static int timeBound(int n, int k)
    /**
       returns the upper bound on the time
       as stated in the paper
     */
    {
	return
	4*n+k*( clg(k)*clg(k)+clg(n-k+1)+6*clg(k))-2;
    }


    // PERMUTATION BSO

    public static int bso(int m, int x)
    /**
     permutation BSO 
     */
 	
    {
	// we search x in binary heap
	if(x<0 || x>= m ) return x;  // arguments outside domain are not permuted 
	int y=0;       // we start from the root of the heap
        int x1= ls(m); // value in the current visited node T[y]
        int m1=m;      // size of visited subtree
        while (x1!=x)
	    {
		if (x<x1)
		    {
			y=l(y); // left child in the binary heap
			m1=ls(m1);
			x1=x1-m1 + ls(m1);   
		    }
		else // x>x1
		    {
			y=r(y); // right child in the binary heap
			m1=rs(m1);
			x1=x1+ls(m1)+1;
		    }
	    }
	return y;
    }


    

    // PERMUTATION REVERSE TO BSO

    public static int osb(int m, int y)
    /**
     permutation reverse to BSO 
     */
 	
    {
	// we search in binary heap
	if(y<0 || y>= m ) return y;  // arguments outside domain are not permuted 
        int levy = lev(y);
        int inlevy = inlev(y); // position of y in its level in current subtree

	// we start from the root of the heap
        int d= pow2(levy-1); // half of the full level size of the level containing y in the currently visited subtree
        int x1= ls(m); // value in the current visited node T[y]
        int m1=m;      // size of currently  visited subtree
        for(int i=0; i<levy; i++) // we go downwards level by level 
	    {
		// here we decide wehter to go left or right
		if (inlevy < d) 
		    {
                        // go left
			m1=ls(m1);
			x1=x1-m1 + ls(m1);   
		    }
		else 
		    {
			// go right
			m1=rs(m1);
			x1=x1+ls(m1)+1;
                        inlevy = inlevy-d; // in the right subtree we subtract the nodes belonging the the left subtree  
		    }
		d=d/2; // the selected subtree contains half of the level from the previous tree
	    }
	return x1;
    }




    ////////////////////////////////////////////////////////////////////////////////////////////////////////////



    public static void main(String[] args)
	throws Exception
    {

	System.out.println("ELEMENTARY FUNCTIONS:");
	for(int x=-1; x<17; x++){
	    System.out.println("clg("+x+") = "+clg(x)
                               +"; flg("+x+") = "+flg(x)
                               +"; h("+x+") = "+h(x)
                               +"; pow2("+x+")= "+pow2(x)
                               +"; fs("+x+")= "+fs(x)
                               +"; ml("+x+")= "+ml(x)
                               +"; ls("+x+")= "+ls(x)
                               +"; rs("+x+")= "+rs(x)
                               +"; l("+x+")= "+l(x)
                               +"; r("+x+")= "+r(x)
                               +"; p("+x+")= "+p(x)
                               +"; lev("+x+")= "+lev(x)
                               +"; inlev("+x+")= "+inlev(x)
			       );
	}


	System.out.println("BSO:");
	for(int m=1; m<= 16 ; m++)
	    {
		System.out.println("m = "+m);
		for(int x=-1; x<= m; x++)
		    System.out.println(
				       "  bso( "+m+" , "+x+" ) = "+bso(m,x)
				       +"; osb( "+m+" , "+x+" ) = "+osb(m,x)
				       );
	    }

        
	int mMax=1000; 

        System.out.println("TESTING: osb(m,bso(m,x)) = x AND bso(m,osb(m,x))=x, FOR 1<= m <="+mMax);
	for(int m=1; m<=mMax; m++)
	    {
	        // System.out.print(" m= "+m+";");
		for(int x=-1; x<= m; x++)
		    {
			if(osb(m,bso(m,x))!= x) 
			    throw new Exception("\n Something wrong: osb("+m+",bso("+m+","+x+"))="+osb(m,bso(m,x)));
			if(bso(m,osb(m,x))!= x) 
			    throw new Exception("\n Something wrong: bso("+m+",osb("+m+","+x+"))="+bso(m,osb(m,x)));
		    }
	    }
	System.out.println("\nOK");

	{
            
	    System.out.println("TESTING: {bso(m,x) | 0<= x <m } = {0, ... , m-1}, FOR 1<= m <="+mMax);
	    for(int m=1; m<=mMax; m++)
		{
		    int[] tab=new int[m];
		    // System.out.print(" m= "+m+";");
		    for(int x=0; x< m; x++) tab[x]=-1; // insert NILs
		    for(int x=0; x< m; x++)
			{
			    int y=bso(m,x);
			    if(tab[y] != -1) 
				throw new Exception("\n Something wrong: bso("+m+","+x+")="+y+
						    " and tab["+y+"]="+tab[y]);
			    tab[y]=x;
			}
		    for(int y=0; y<m; y++) 
			if( tab[y] == -1 ) 
			    throw new Exception("\n Something wrong: tab["+y+"] remained "+tab[y]);
		}
	    System.out.println("\nOK");

	    System.out.println("TESTING: {osb(m,x) | 0<= x <m } = {0, ... , m-1}, FOR 1<= m <="+mMax);
	    for(int m=1; m<=mMax; m++)
		{
		    int[] tab=new int[m];
		    // System.out.print(" m= "+m+";");
		    for(int x=0; x< m; x++) tab[x]=-1; // insert NILs
		    for(int x=0; x< m; x++)
			{
			    int y=osb(m,x);
			    if(tab[y] != -1) 
				throw new Exception("\n Something wrong: bso("+m+","+x+")="+y+
						    " and tab["+y+"]="+tab[y]);
			    tab[y]=x;
			}
		    for(int y=0; y<m; y++) 
			if( tab[y] == -1 ) 
			    throw new Exception("\n Something wrong: tab["+y+"] remained "+tab[y]);
		}
	    System.out.println("\nOK");

	}
	
	System.out.println("\nTESTING at(l,i)");
	for(int l=0; l<5; l++)
	    {
		for(int i=0; i<pow2(l); i++)
		    System.out.print("at("+l+","+i+") = "+at(l,i)+"; ");
		System.out.println();
	    }

	System.out.println("\nTESTING levsize(m,l)");
	for(int m=1; m<8; m++)
	    {
		for(int l=-1; l<=h(m); l++)
		    System.out.print("levsize("+m+","+l+")="+levsize(m,l)+"; ");
		System.out.println();
	    }

    }

}