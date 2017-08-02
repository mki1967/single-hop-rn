/**
 *
 *    Naive simulation of  merging algorithm in
 *    the single-hop radio network where each station stores $k$ keys.
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
 *    The author can be reached at kik@im.pwr.wroc.pl
 *
 */
/**
 *
 * This program is a naive simulation of the
 * merging algorithm 
 * described in the DELIS technical report:
 *
 *    M.Kik "Sorting Long Sequence in a Single-hop Radio Network".
 *    DELIS-TR-0239 (http://delis.upb.de/paper/DELIS-TR-0239.pdf)
 *
 * It is intended to verify the correctness and demonstrate implementability
 * of the algorithm.
 * This source file also explains some technical details that were skipped
 * in the technical report.
 * 
 * The procedures:
 * init, findPartners, lUpdate, rUpdate, tryRanking, rankUnsplit,  
 * rank and merge
 * in the class RadioNetwork
 * are described in the technical report.
 * The remaining procedures are providing enviroment for the simulations.
 *
 * Note that it is sequential simulation and may be very slow
 * for large values $k$ and $m$.
 *
 */



/*
 * LongSort.java
 *
 * Created on 16 listopad 2005, 15:23
 */

/**
 *
 * @author kik
 */


import java.util.*;




public class LongSort {
    static final int NIL=-1;
    static final int COLLISION=-2;
    
    
    
    /** Creates a new instance of LongSort */
    public LongSort() {
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        
        int m=8, k=4;
        
        try{
            m= Integer.parseInt(args[0]);
            k= Integer.parseInt(args[1]);
        } catch(Exception e) {
            System.out.println("PROGRAM SHOULD BE INVOKED WITH TWO INTEGERS $m$ AND $k$:");
            System.out.println("   java LongSort $m$ $k$");
            System.exit(-1);
        }
        
        
        RadioNetwork rn=new RadioNetwork(m,k);
        
        //       System.out.println("\nKEYS:"); rn.printTable(rn.a);
        //       System.out.println();  rn.printTable(rn.b);
        
        
        rn.merge(rn.a, rn.b);
        
        //        System.out.println("\n\nRESULT:"); rn.printTable(rn.a);
        //        System.out.println();  rn.printTable(rn.b);
        
        System.out.println("\n\nOutputOk = "+rn.outputOK());
        System.out.println("\nSE = "+rn.maxSE()+", bound = "+rn.upBoundSE());
        System.out.println("LE = "+rn.maxLE()+", bound = "+rn.upBoundLE());
        System.out.println("clock ="+rn.clock+", bound = "+rn.boundT());
        
    }
    
}



class Station{
    
    
    // Variables of the algorithm
    
    
    int[] key;     // key[0..k+1]
    int[] rank;    // rank[1..k]
    int[] idx;     // idx[1..k]
    int[] newKey;  // new[1..k]
    
    
    
    int[] lPartner; // $<x,f,l>$
    int[] rPartner; // $<x,f,l>$
    int lTimer;
    int rTimer;
    int lRank;
    int rRank;
    
    boolean split;
    
    int received; // last received message
    int f,l; // last received range in FindPartners
    int v; // currently considered key of $b$-sequence in TryRanking
    
    // Variables for statistics
    
    int le; // energy for listening
    int se; // energy for sending
    
    
    Station(int k)
    // Constructs new station for $k$ keys
    {
        key= new int[k+2];   // key[0...k+1]
        rank= new int[k+1];  // rank[1...k]
        idx= new int[k+1];   // idx[1...k]
        newKey= new int[k+1]; // new[1...k]
        
    }
    
    
    void listen(Channel c) {
        received= c.message;
        le++;
    }
    
    void send(Channel c, int message) {
        c.broadcast(message);
        se++;
    }
    
    
    // procedures for debugging
    
    void printTable(int t[]) {
        int k= key.length-2;
        for(int i=1; i<=k; i++) {
            System.out.print(t[i]+",");
        }
        
    }
    
    void printKeys() {
        printTable(key);
    }
    
    void printEKeys(){
        int k=key.length-2;
        System.out.print(key[0]+",");
        printTable(key);
        System.out.print(key[k+1]);
    }
    
    void printIdx() {
        printTable(idx);
    }
    
    void printNew() {
        printTable(newKey);
    }
    
    void printRank() {
        printTable(rank);
    }
    
    void printPartners() {
        if(lPartner==null)
            System.out.print("[_  ");
        else
            System.out.print("[" // +lPartner[0]+":"
                    +lPartner[1]+"-"+lPartner[2]+" ");
        if(rPartner==null)
            System.out.print("_]");
        else
            System.out.print(  //rPartner[0]+":"+
                    rPartner[1]+"-"+rPartner[2]+"]");
        
    }
    
    
}


class Channel{
    int message;
    
    
    public Channel() {
        clean();
    }
    
    void clean() {
        message=LongSort.NIL;
    }
    
    void broadcast(int m) {
        if(message == LongSort.NIL)
            message=m;
        else
            message= LongSort.COLLISION;
        
    }
    
}


class RadioNetwork{
    
    Station[] a;  // stations for a-sequence
    Station[] b;  // stations for b-sequence
    Station[] c;  // concatenated stations of a-sequence and b-sequence
    
    Channel channel; // single channel network
    int clock;      // number of time slot
    
    
    RadioNetwork(int m, int k)
    // Constructs a network for merging two sequences of size $mk$
    {
        channel=new Channel();
        
        a= new Station[m+1]; // a[1] ... a[m]
        b= new Station[m+1]; // b[1] ... b[m]
        
        for(int i=1; i<=m; i++) {
            a[i]= new Station(k);
            b[i]= new Station(k);
        }
        
        c=new Station[2*m+1];
        
        for(int i=1; i<=m; i++) {
            c[i]=a[i];
            c[m+i]=b[i];
        }
        
        
        generateInput();
    }
    
    
    void generateInput()
    // Generates sorted a-sequence and b-sequence
    // by randomly distributing the sequence 1,...,2km
    // between a-seuence and b-sequence
    {
        long seed=System.currentTimeMillis();
        // seed=1132341389132L;
        Random random=new Random(seed);
        
        System.out.println("Seed ="+seed);
        
        // retrieve values $m$ and $k$
        int m= a.length-1;
        int k= a[1].key.length-2;
        
        int ia=0, ib=0, j=1;
        
        while(ia<m*k && ib<m*k) {
            if(random.nextBoolean()) {
                ia++;
                a[(ia-1)/k+1].key[(ia-1)%k +1]=j;
            } else {
                ib++;
                b[(ib-1)/k+1].key[(ib-1)%k +1]=j;
            }
            j++;
        }
        
        while(ia<m*k) {
            ia++;
            a[(ia-1)/k+1].key[(ia-1)%k +1]=j;
            j++;
        }
        
        while(ib<m*k) {
            ib++;
            b[(ib-1)/k+1].key[(ib-1)%k +1]=j;
            j++;
        }
        
    }
    
    
    
    // Simulation procedures
    
    
    void newTimeSlot() {
        channel.clean();
        clock++;
    }
    
    
    
    // statistics
    
    int maxLE() {
        int mx=c[1].le;
        
        for(int i=1; i<= c.length-1; i++)
            if(mx<c[i].le) mx=c[i].le;
        return mx;
    }
    
    int upBoundLE()
    // upper bount from technical report
    {
        int m=a.length-1;
        int k=a[1].key.length-2;
        
        return 4*k+4*T.h(m,1);
    }
    
    int maxSE() {
        int mx=c[1].se;
        
        for(int i=1; i<= c.length-1; i++)
            if(mx<c[i].se) mx=c[i].se;
        return mx;
    }
    
    int upBoundSE()
    // upper bount from technical report
    {
        int m=a.length-1;
        int k=a[1].key.length-2;
        
        return 4*k+2;
    }
    
    
    int boundT()
    // upper bound from technical report
    {
        int m=a.length-1;
        int k=a[1].key.length-2;
        
        return 6*m*k+4*m-4;
    }
    
    
    // procedures for debugging
    
    void printTable(Station[] x) {
        int m=x.length-1;
        for(int i=1; i<=m; i++) {
            System.out.print("|");
            x[i].printKeys();
        }
    }
    
    void printETable(Station[] x) {
        int m=x.length-1;
        for(int i=1; i<=m; i++) {
            System.out.print("|");
            x[i].printEKeys();
        }
    }
    
    
    
    
    void printTableIdx(Station[] x) {
        int m=x.length-1;
        for(int i=1; i<=m; i++) {
            System.out.print("|");
            x[i].printIdx();
            
        }
    }
    
    void printTableNew(Station[] x) {
        int m=x.length-1;
        for(int i=1; i<=m; i++) {
            System.out.print("|");
            x[i].printNew();
            
        }
    }
    
    void printTableRank(Station[] x) {
        int m=x.length-1;
        for(int i=1; i<=m; i++) {
            System.out.print("|");
            x[i].printRank();
            
        }
    }
    
    
    void printPartners(Station[] x) {
        int m=x.length-1;
        for(int i=1; i<=m; i++) {
            x[i].printPartners();
            
        }
    }
    
    boolean outputOK() {
        int m2=c.length-1;
        int k=c[1].key.length-2;
        int j=1;
        
        for(int i=1; i<=m2; i++)
            for(int r=1; r<=k; r++) {
            if(c[i].key[r]!=j) return false;
            j++;
            }
        return true;
    }
    
    
    
    
    // Procedures of the algorithm described in the technical report
    
    
    // procedure Init from the technical report
    
    void init(Station[] a) {
        int m=a.length-1;         // retrieve m
        int k=a[1].key.length-2; // retrieve k
        
        a[1].key[0]= 0;         //Integer.MIN_VALUE; // $-\infty$
        a[m].key[k+1]= 2*m*k+1; //Integer.MAX_VALUE; // $+\infty$
        for(int i=1; i<=m-1; i++) {
            newTimeSlot();
            a[i].send(channel, a[i].key[k]);
            a[i+1].listen(channel);
            a[i+1].key[0]=a[i+1].received;
        }
        
        for(int i=1; i<=m-1; i++) {
            newTimeSlot();
            a[i+1].send(channel, a[i+1].key[1]);
            a[i].listen(channel);
            a[i].key[k+1]=a[i].received;
        }
    }
    
    
    // procedure FindPartners from the technical report
    
    void findPartners(Station[] a,Station[] b) {
        int m=a.length-1;         // retrieve m
        int k=a[1].key.length-2; // retrieve k
        
        for(int i=1; i<=m; i++) {
            a[i].lTimer = a[i].rTimer = 1 ;
            a[i].lRank =  a[i].rRank = 0;
            a[i].lPartner = a[i].rPartner = null;
            a[i].split = false;
        }
        
        
        for(int d=1; d<=m; d++) {
            // int x=1;
            // while(d != T.p(m,x)) x++;
            int x=T.pReverse(m, d);
            
            
            newTimeSlot();
            b[x].send(channel, b[x].key[1]); // b[i] broadcasts its leftmost key
            for(int i=1; i<=m; i++) {
                if(a[i].lTimer==d || a[i].rTimer==d) {
                    a[i].listen(channel);
                    a[i].f=a[i].received; // remember the first received key
                }
            }
            
            newTimeSlot();
            b[x].send(channel, b[x].key[k]); // b[i] broadcasts its rightmost key
            for(int i=1; i<=m; i++) {
                if(a[i].lTimer==d || a[i].rTimer==d) {
                    a[i].listen(channel);
                    a[i].l=a[i].received; // remember the second received key
                }
            }
            
            for(int i=1; i<=m; i++) {
                boolean wasLActive= (a[i].lTimer== d);
                boolean wasRActive= (a[i].rTimer== d);
                if(wasLActive) lUpdate(x, a[i]);
                if(wasRActive) rUpdate(x, a[i]);
            }
            
        }
        
        // compute splits
        for(int i=1; i<=m; i++)
            a[i].split =
                    (a[i].lPartner==null && a[i].rPartner== null && a[i].lRank<a[i].rRank)
                    ||
                    (a[i].lPartner!=null && a[i].rPartner!=null && a[i].lPartner[0]+1<a[i].rPartner[0])
                    ||
                    (a[i].lPartner!=null && a[i].rPartner==null && a[i].lPartner[0]*k<a[i].rRank)
                    ||
                    (a[i].lPartner==null && a[i].rPartner!=null && a[i].lRank<(a[i].rPartner[0]-1)*k)
                    ;
    }
    
    
    // We replace single Update(...) from technical report by lUpdate(...) and rUpdate(...)
    // since there are not references to variables of simple data types in Java
    
    void lUpdate(int x, Station a) {
        int k=a.key.length-2; // retrieve k
        int m= this.a.length-1; // retrieve m from the  global table a
        
        if(a.f<a.key[1] && a.key[1]<a.l) {
            a.lPartner= new int[3];
            a.lPartner[0]= x;
            a.lPartner[1]= a.f;
            a.lPartner[2]= a.l;
            a.lTimer=LongSort.NIL;
        } else
            if(a.key[1]<a.f) {
            a.lTimer= T.p(m, T.l(m,x)); // preodrer index of the left son of $x$
            
            } else
                if(a.l<a.key[1]) {
            a.lTimer=T.p(m, T.r(m,x));  // preodrer index of the right son of $x$
            a.lRank=x*k;
                }
        
        
    }
    
    void rUpdate(int x, Station a) {
        int k=a.key.length-2; // retrieve k
        int m= this.a.length-1; // retrieve m from the table a
        
        if(a.f<a.key[k] && a.key[k]<a.l) {
            a.rPartner= new int[3];
            a.rPartner[0]= x;
            a.rPartner[1]= a.f;
            a.rPartner[2]= a.l;
            a.rTimer= LongSort.NIL;
        } else
            if(a.key[k]<a.f) {
            a.rTimer= T.p(m, T.l(m,x)); // preodrer index of the left son of $x$
            
            } else
                if(a.l<a.key[k]) {
            a.rTimer=T.p(m, T.r(m,x));  // preodrer index of the right son of $x$
            a.rRank=x*k;
                }
        
        
    }
    
    
    // procedure TryRanking from the technical report
    // We apply the modifications suggested in paragraph
    // "Further Improvements" of the technical report
    // (i.e. $b$-stations do not repeat broadcasting of their border keys).
    
    // The auxiliary procedure replayOfA(i) corresonds to the code fragment of TryRanking
    // in which one of the partners of b[i] (i.e. a[j]) informs
    // b[i] about the rank of b[i].key[r] in the $a$-sequence
    // assuming, that the value of b[i].key[r] is stored in a[j].v
    
    void replayOfA(Station[] a, int i) {
        int m=a.length-1;
        int k=a[1].key.length-2;
        
        for(int j=1; j<=m; j++)
            if( a[j].key[0]< a[j].v &&
                (
                (a[j].lPartner!=null && a[j].lPartner[0]==i) ||
                (a[j].rPartner!=null && a[j].rPartner[0]==i)
                )
                ) {
            int s=LongSort.NIL;
            for(int s1=1; s==LongSort.NIL && s1<=k; s1++)
                if(a[j].key[s1-1]<a[j].v && a[j].v<a[j].key[s1])
                    s=s1; // a[j] contains successor of b[i].key[r]
            if(
                    s==LongSort.NIL &&
                    (
                    (a[j].lPartner!=null && a[j].lPartner[0]==i
                    && a[j].lPartner[2]<a[j].key[k+1])
                    ||
                    (a[j].rPartner!=null && a[j].rPartner[0]==i
                    && a[j].rPartner[2]<a[j].key[k+1])
                    )
                    ) {
                // successor of b[i][r] is either in a[j+1] which is not a partner of b[i] or $+\infty$
                s=k+1;
            }
            if(s!=LongSort.NIL) // a[j] should broadcast
                a[j].send(channel, (j-1)*k+s-1);
            
            }
        
    }
    
    
    void tryRanking(Station[] a, Station b[]) {
        int m=a.length-1; // retrieve $m$
        int k=a[1].key.length-2; // retrieve $k$
        
        init(a);
        findPartners(a,b);
        
        for(int i=1; i<=m; i++) {
            int r=1;  // now we consider the key b[i].key[r], for r=1
            
            
            for(int j=1; j<=m; j++)
                if(
                    (a[j].lPartner!=null && a[j].lPartner[0]==i) ||
                    (a[j].rPartner!=null && a[j].rPartner[0]==i)
                    ) {
                a[j].v=
                        (a[j].lPartner!=null && a[j].lPartner[0]==i)?
                            a[j].lPartner[1]: a[j].rPartner[1]; // == b[i].key[1]
                if(a[j].split== false) // update ranks in a[j]
                    for(int s=1; s<=k; s++)
                        if(a[j].v < a[j].key[s]) a[j].rank[s]=(i-1)*k+r;
                
                }
            
            // react to the key b[i].key[r]
            newTimeSlot();
            replayOfA(a, i);
            b[i].listen(channel);
            if(b[i].received != LongSort.NIL) b[i].rank[r]=b[i].received;
            
            
            
            // the keys b[i].key[2...k-1] must be broadcast
            for(r=2; r<=k-1; r++) {
                newTimeSlot();
                b[i].send(channel,b[i].key[r]);
                for(int j=1; j<=m; j++)
                    if(
                        (a[j].lPartner!=null && a[j].lPartner[0]==i) ||
                        (a[j].rPartner!=null && a[j].rPartner[0]==i)
                        ) {
                    a[j].listen(channel);
                    a[j].v=a[j].received;
                    if(a[j].split== false) // update ranks in a[j]
                        for(int s=1; s<=k; s++)
                            if(a[j].v<a[j].key[s]) a[j].rank[s]=(i-1)*k+r;
                    
                    }
                
                // react to the key b[i].key[r]
                newTimeSlot();
                replayOfA(a, i);
                b[i].listen(channel);
                if(b[i].received!=LongSort.NIL) b[i].rank[r]=b[i].received;
                
            }
            
            
            // now r==k and we consider the key b[i].key[r].
            for(int j=1; j<=m; j++)
                if(
                    (a[j].lPartner!=null && a[j].lPartner[0]==i) ||
                    (a[j].rPartner!=null && a[j].rPartner[0]==i)
                    ) {
                a[j].v=
                        (a[j].lPartner!=null && a[j].lPartner[0]==i)?
                            a[j].lPartner[2]: a[j].rPartner[2]; // == b[i].key[k]
                if(a[j].split== false) // update ranks in a[j]
                    for(int s=1; s<=k; s++)
                        if(a[j].v < a[j].key[s]) a[j].rank[s]=(i-1)*k+r;
                
                }
            newTimeSlot();
            replayOfA(a, i);
            b[i].listen(channel);
            if(b[i].received!=LongSort.NIL) b[i].rank[r]=b[i].received;
            
            
        }
        
        for(int i=1; i<=m; i++) rankUnsplit(a[i]);
        
    }
    
    
    
    // procedure RankUnsplit from the technical report
    
    void rankUnsplit(Station a) {
        int k=a.key.length-2;
        if(a.split== false) {
            if(a.lPartner== null && a.rPartner== null)
                for(int r=1; r<=k; r++)
                    a.rank[r]= a.lRank;
            else if(a.lPartner== null) {
                int last=1;
                while(a.key[last+1]<a.rPartner[1]) last++;
                for(int r=1; r<=last; r++ )
                    a.rank[r]=a.lRank;
            }
        }
    }
    
    
    
    // procedure Rank from the technical report
    
    void rank(Station[] a, Station[] b) {
        int m=a.length-1;
        int k=a[1].key.length-2;
        
        for(int i=1; i<=m; i++)
            for(int r=1; r<=k; r++) {
            a[i].rank[r]= LongSort.NIL;
            b[i].rank[r]= LongSort.NIL;
            }
        
        tryRanking(a,b);
        
        //        System.out.println("\nRANKS 1:"); printTableRank(a);
        //        System.out.println(); printTableRank(b);
        
        tryRanking(b,a);
        
        //        System.out.println("\nRANKS 2:"); printTableRank(a);
        //        System.out.println();  printTableRank(b);
        
        //        System.out.println("\nPARTNERS 2:"); printPartners(a);
        //        System.out.println(); printPartners(b);
        
        
    }
    
    // procedure Merge from the technical report
    
    void merge(Station[] a, Station[] b) {
        int m= a.length-1;
        int k= a[1].key.length-2;
        
        rank(a,b);
        
        
        
        for(int i=1; i<=m; i++) {
            for(int r=1; r<=k; r++)
                a[i].idx[r]=(i-1)*k+r+a[i].rank[r];
            for(int r=1; r<=k; r++)
                b[i].idx[r]=(i-1)*k+r+b[i].rank[r];
        }
        
        Station[] c=new Station[2*m+1];
        for(int i=1; i<=m; i++) {
            c[i]=a[i];
            c[m+i]=b[i];
        }
        
        //      System.out.println("\nIDX:"); printTableIdx(a);
        //      System.out.println(); printTableIdx(b);
        
        
        
        for(int t=1; t<= 2*m*k; t++) {
            newTimeSlot();
            for(int i=1; i<=2*m; i++)
                for(int r=1; r<=k; r++)
                    if(c[i].idx[r]==t)
                        c[i].send(channel, c[i].key[r]);
            int t1=(t-1)/k+1;
            int r=t-(t1-1)*k;
            c[t1].listen(channel);
            c[t1].newKey[r]=c[t1].received;
        }
        
        
        
 /*
        for(int i=1; i<= 2*m; i++)
            for(int r=1; r<=k; r++) {
            newTimeSlot();
            for(int j=1; j<=2*m; j++)
                for(int r1=1; r1<=k; r1++)
                    if(c[j].idx[r1]== (i-1)*k+r)
                        c[j].send(channel,c[j].key[r1]);
            c[i].listen(channel);
            c[i].newKey[r]=c[i].received;
            }
  
  */
        
        
        for(int i=1; i<= 2*m; i++)
            for(int r=1; r<=k; r++)
                c[i].key[r]=c[i].newKey[r];
    }
    
}






/////////////////////////////////////////////////////////////////////

class T{
    // auxiliary functions for the tree $T_m$ from the paper
    
    public static int power(int x, int y)
    // x>0, y>=0
    {
        int z=1;
        int k=1;
        while (k<= y) {
            k++;
            z*=x;
        }
        return z;
    }
    
    public static int height(int m)
    // height of $T_m$
    {
        int k=0;
        int x=1;
        
        while (x<m+1) {
            k++;
            x*=2;
        }
        return k;
    }
    
    
    public static int h(int m, int i)
    // sequence h(m,i) from the paper
    {
        if(i==0) return m;
        return height(h(m, i-1));
    }
    
    
    public static int g(int m, int i)
    // the functiom g(m,i) from the paper
    {
        int x=h(m,i);
        return  (m+x-1)/x;
    }
    
    public static int lStar(int m)
    // $l^*$ from the paper
    {
        int i=0;
        while (h(m,i)>2) i++;
        return i;
        
    }
    
    public static int fullSize(int h)
    // size of full tree of height h
    {
        return power(2,h)-1;
    }
    
    public static int missingLeaves(int m)
    // number of rightmost missing leaves in T_m
    {
        return fullSize(height(m))-m;
    }
    
    
    public static int leftSubtree(int m)
    // size of the left subtree of T_m
    {
        if (m<=1) return 0;
        // m>1
        int ml=missingLeaves(m);
        int h=height(m)-1; // height of left subtree
        int s=fullSize(h); // size of full subtree
        int l=power(2,h-1); // leaves in full subtree
        if (ml>l) s-= (ml-l); // some missing leaves are in left subtree
        return s;
    }
    
    public static int rightSubtree(int m)
    // size of the right subtree of T_m
    {
        return m-1-leftSubtree(m);
    }
    
    public static int root(int m)
    // root of T_m
    {
        return leftSubtree(m)+1;
    }
    
    
    public static int l(int m, int x)
    // left child of x in T_m
    {
        int x1=root(m); // value of current visited node
        int m1=m; // size of visited subtree
        while (x1!=x) {
            if (x<x1) {
                m1=leftSubtree(m1);
                x1=x1-m1+root(m1)-1;
            } else // x>x1
            {
                m1=rightSubtree(m1);
                x1=x1+root(m1);
            }
        }
        
        m1=leftSubtree(m1);
        if(m1>0) return x1-m1+root(m1)-1;
        else return 0;
        
    }
    
    public static int r(int m, int x)
    // right child of x in T_m
    {
        int x1=root(m); // value of current visited node
        int m1=m; // size of visited subtree
        while (x1!=x) {
            if (x<x1) {
                m1=leftSubtree(m1);
                x1=x1-m1+root(m1)-1;
            } else // x>x1
            {
                m1=rightSubtree(m1);
                x1=x1+root(m1);
            }
        }
        
        m1=rightSubtree(m1);
        if(m1>0) return x1+root(m1);
        else return 0;
        
    }
    
    public static int p(int m, int x)
    // preorder index of x in T_m
    {
        // we search x in binary heap
        if(x==0) return 0; // NILs are indexe arbitrarily
        int i=1; // we start from the root
        int x1=root(m); // value of current visited node
        int m1=m; // size of visited subtree
        while (x1!=x) {
            if (x<x1) {
                i=2*i; // left child in the binary heap
                m1=leftSubtree(m1);
                x1=x1-m1+root(m1)-1;
            } else // x>x1
            {
                i=2*i+1; // right child in the binary heap
                m1=rightSubtree(m1);
                x1=x1+root(m1);
            }
        }
        return i;
        
    }
    
    
    public static int pReverse(int m, int y)
    // y>=1 is postorder number of node in T_m; return inorder number
    {
        if(y==1) return root(m);
        else {
            if(y%2 == 0) return l(m, pReverse(m, y/2));
            else return r(m, pReverse(m, y/2));
        }
    }
    
    
    
}



