/**
 *
 *    Naive simulations of  merging and merge-sort algorithms in
 *    the single-hop radio network
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
 * (single channel versions of) merging and merge-sort algorithms 
 * described in the DELIS technical report:
 * 
 *    M.Kik "Merging and Merge-sort in a Single-hop Radio Network".
 *    DELIS-TR-0050 (http://delis.upb.de/paper/DELIS-TR-0050.pdf)
 *
 * It is intended to verify the correctness and demonstrate implementability
 * of the algorithms.
 * This source file also explains some technical details that were skipped
 * in the technical report.
 *
 * The user may select the algorithm to be simulated by 
 * uncommenting one of the main() methods in RadioNetwork class
 * and commenting another, and then within this main() method 
 * uncommenting one algorithm and commenting the remaining algorithms.
 * One of the main methods is devoted to merging while the other is
 * devoted to merge-sort algorithms.
 *
 * Merging algorithms are implemented in the methods:
 *   Merge() 
 *   Merge1()
 *   Merge2()
 *
 * Merge-sort algorithms are implemented in the methods:
 *   MergeSort()
 *   MergeSort1()
 *   MergeSort2()
 *
 *
 *
 * Note that this simulation may be very slow for large data sizes.
 * This is due to the sequential simulation of parallel listening of single message
 * and exhaustive checking of each sensor whether it should be active in given time slot.
 *
 */


import java.util.*;

public class RadioNetwork {
    Channel channel;
    Sensor[] sensor;
    int clock;
    
    RadioNetwork(int n) {
        clock=1;
        channel=new Channel();
        sensor= new Sensor[n+1]; // tables are indexed from 1
        int i;
        for(i=1; i<=n; i++) sensor[i]=new Sensor();
    }
    
    
    // Algorithms
    
    public void Rank(Sensor a[], Sensor b[])
    // Rank sensors a[1] ... a[m] in the sequence <b[1] .. b[m]>
    {
        int m=a.length-1; // sensors are numbered from 1
        
        int i;
        for(i=1; i<= m; i++) {
            a[i].timer=1;
            a[i].rank=0;
        }
        
        
        int d,x,j;
        for(d=1; d<= m; d++) {
            x=T.pReverse(m,d);
            
            // SEND
            int[] message=new int[1];
            message[0]=b[x].key;
            b[x].send(message, channel);
            
            // RECEIVE
            for(j=1; j<= m; j++) {
                if(a[j].timer==d) {
                    a[j].listen(channel);
                    if (a[j].key<a[j].rcvdMsg[0])
                        a[j].timer=T.p(m, T.l(m,x));
                    else {
                        a[j].timer=T.p(m, T.r(m,x));
                        a[j].rank=x;
                    }
                }
            }
            
            channel.clean();
            clock++;
        }
        
    }
    
    
    // Merging with energetic cost: \lg m+3
    
    public void Merge(Sensor[] a, Sensor[] b) {
        int m=a.length-1;
        
        Rank(a,b);
        Rank(b,a);
        
        int i;
        for(i=1; i<=m; i++) {
            a[i].idx=i+a[i].rank;
            b[i].idx=i+b[i].rank;
        }
        
        Sensor[] c= new Sensor[2*m+1];
        for(i=1; i<=m; i++) c[i]=a[i];
        for(i=1; i<=m; i++) c[i+m]=b[i];
        
        int t;
        for(t=1; t<=2*m; t++) {
            // SEND
            for(i=1; i<=2*m; i++)
                if(c[i].idx==t) {
                    int[] message=new int[1];
                    message[0]=c[i].key;
                    c[i].send(message, channel);
                }
            if(channel.message==Channel.COLLISION) {
                System.out.println("COLLISION");
                for(i=1; i<=2*m; i++)
                    System.out.println
                    (i+" "+c[i].idx+" "+c[i].rank+" "+c[i].key);
            }
            
            // RECEIVE
            c[t].listen(channel);
            c[t].newkey= c[t].rcvdMsg[0];
            
            channel.clean();
            clock++;
        }
        
        for(i=1; i<=2*m; i++) c[i].key=c[i].newkey;
        
    }
    
    
    public void MergeSort(Sensor[] s) {
        int m=s.length-1; // m is a power of two
        
        if(m>1) {
            Sensor[] c1=new Sensor[m/2+1];
            int i;
            for(i=1; i<= m/2; i++) c1[i]=s[i];
            MergeSort(c1);
            
            Sensor[] c2=new Sensor[m/2+1];
            for(i=1; i<= m/2; i++) c2[i]=s[i+m/2];
            MergeSort(c2);
            Merge(c1,c2);
        }
    }
    
    
    /// Regroup
    
    public void Regroup(int i, Sensor[] c1, Sensor[] d1) {
        int m=c1.length-1;
        int j,k;
        
        //  	System.out.println("Regroup: "+m+" "+i+" "+T.h(m,i));
        
        Sensor[][] c= new Sensor[T.g(m,i)+1][T.h(m,i)+1];
        for(j=1; j<= T.g(m,i); j++)
            for(k=1; k<=T.h(m,i); k++)
                if(T.alpha(m,i,j,k)<=m) c[j][k]=c1[T.alpha(m,i,j,k)];
                else  c[j][k]=d1[T.alpha(m,i,j,k)-m];
        
        Sensor[][] d= new Sensor[T.g(m,i-1)+1][T.h(m,i-1)+1];
        for(j=1; j<= T.g(m,i-1); j++)
            for(k=1; k<=T.h(m,i-1); k++)
                if(T.alpha(m,i-1,j,k)<=m) d[j][k]=d1[T.alpha(m,i-1,j,k)];
        
        
        
        
        // Phase 1
        
        for(j=1; j<=T.g(m,i); j++) {
            c[j][1].group1=c[j][1].group;
            c[j][1].key1=c[j][1].key;
            c[j][1].timer=1;
            c[j][1].rank1=0;
        }
        
        
        int g,l,v;
        for(l=1; l<= T.h(m,i); l++) {
            for(v=T.power(2,l-1); v<= Math.min(T.power(2,l)-1,T.h(m,i-1)); v++)
                for(g=1; g<=T.g(m,i-1); g++) {
                    // SEND
                    int x=T.pReverse(T.h(m,i-1),v);
                    if(d[g][x]!=null) {
                        int[] message = new int[1];
                        message[0]=d[g][x].key;
                        d[g][x].send(message,channel);
                    }
                    
                    // RECEIVE
                    for(j=1; j<= T.g(m,i); j++)
                        if(c[j][l].group1==g && c[j][l].timer==v) {
                            c[j][l].listen(channel);
                            if( c[j][l].rcvdMsg == null ||
                            c[j][l].key1<c[j][l].rcvdMsg[0] ) {
                                c[j][l].timer=
                                T.p(T.h(m,i-1),T.l(T.h(m,i-1),x));
                            }
                            else {
                                c[j][l].timer=
                                T.p(T.h(m,i-1),T.r(T.h(m,i-1),x));
                                c[j][l].rank1=T.alpha(m,i-1,g,x);
                            }
                        }
                    
                    channel.clean();
                    clock++;
                    
                }
            
            // transfer task to the next slave
            
            if(l<= T.h(m,i)-1)
                for(j=1; j<=T.g(m,i); j++) {
                    // SEND
                    int[] message=new int[4];
                    message[0]=c[j][l].timer;
                    message[1]=c[j][l].rank1;
                    message[2]=c[j][l].group1;
                    message[3]=c[j][l].key1;
                    c[j][l].send(message, channel);
                    
                    // RECEIVE
                    c[j][l+1].listen(channel);
                    c[j][l+1].timer  =c[j][l+1].rcvdMsg[0];
                    c[j][l+1].rank1  =c[j][l+1].rcvdMsg[1];
                    c[j][l+1].group1 =c[j][l+1].rcvdMsg[2];
                    c[j][l+1].key1   =c[j][l+1].rcvdMsg[3];
                    
                    channel.clean();
                    clock++;
                }
            
        } // for
        
        
        // TEST
        // 	int ph1=(T.g(m,i-1)*T.h(m,i-1)+T.g(m,i)*(T.h(m,i)-1));
        // 	System.out.println("After Phase 1: clock="+clock+
        // 			    " estimated by: "+ ph1
        // 			    );
        
        
        
        // Phase 2
        
        for(j=1; j<= T.g(m,i); j++) c[j][1].winner=true;
        
        for(j=1; j<=T.g(m,i); j++) {
            // SEND
            int[] message=new int[1];
            message[0]=c[j][T.h(m,i)].rank1;
            c[j][T.h(m,i)].send(message, channel);
            
            // RECEIVE
            c[j][1].listen(channel);
            c[j][1].rank=c[j][1].rcvdMsg[0];
            
            if(j>1) {
                c[j-1][1].listen(channel);
                if(c[j-1][1].rank==c[j-1][1].rcvdMsg[0]) c[j-1][1].winner=false;
            }
            
            channel.clean();
            clock++;
        }
        
        
        
        // Phase 3
        
        for(l=1; l<=m; l++) d1[l].group=-1; // -1 == NIL
        d1[1].group=0;
        
        for(l=1; l<=m; l++) {
            // SEND
            for(j=1; j<=T.g(m,i); j++)
                if(c[j][1].winner && c[j][1].rank==l-1) {
                    int[] message=new int[1];
                    message[0]=j;
                    c[j][1].send(message,channel);
                }
            
            // RECEIVE
            d1[l].listen(channel);
            if(d1[l].rcvdMsg!= null) {
                d1[l].group=d1[l].rcvdMsg[0];
            }
            
            channel.clean();
            clock++;
        }
        
        
        
        // Phase 4
        for(l=1; l<= m-1; l++) {
            // SEND
            int[] message= new int[1];
            message[0]=d1[l].group;
            d1[l].send(message,channel);
            
            // RECEIVE
            if(d1[l+1].group == -1) {
                d1[l+1].listen(channel);
                d1[l+1].group=d1[l+1].rcvdMsg[0];
            }
            
            channel.clean();
            clock++;
        }
        
    }
    
    
    // Rank1
    
    public void Rank1(Sensor[] a, Sensor[] b) {
        int m=a.length-1;
        int i,j,k;
        for(i=1; i<=m; i++) b[i].group=1;
        
        Regroup(1,b,a);
        
        
        // Phase B
        
        Sensor[][] b1=new Sensor[T.g(m,1)+1][T.h(m,1)+1];
        for(j=1; j<=T.g(m,1); j++)
            for(k=1; k<=T.h(m,1); k++)
                if(T.alpha(m,1,j,k)<=m) b1[j][k]=b[T.alpha(m,1,j,k)];
        
        int g,l;
        
        
        for(l=1; l<= m; l++) {
            a[l].rank=0;
            a[l].timer=1;
        }
        
        for(g=1; g<=T.g(m,1); g++) {
            for(k=1; k<=T.h(m,1); k++) {
                
                // SEND
                int x=T.pReverse(T.h(m,1),k);
                if(b1[g][x]!=null) {
                    int[] message=new int[1];
                    message[0]=b1[g][x].key;
                    b1[g][x].send(message,channel);
                }
                
                //RECEIVE
                for(l=1; l<=m; l++)
                    if(a[l].group==g && a[l].timer==k) {
                        a[l].listen(channel);
                        if(a[l].rcvdMsg==null
                        || a[l].key<a[l].rcvdMsg[0]) {
                            a[l].timer=T.p(T.h(m,1),T.l(T.h(m,1),x));
                        }
                        else {
                            a[l].rank=T.alpha(m,1,g,x);
                            a[l].timer=T.p(T.h(m,1),T.r(T.h(m,1),x));
                        }
                    }
                
                channel.clean();
                clock++;
            }
        }
        
        
    }
    
    
    
    //  Merging with energetic cost: O(\lg\lg m)
    
    public void Merge1(Sensor[] a, Sensor[] b) {
        int m=a.length-1;
        
        Rank1(a,b);
        Rank1(b,a);
        
        int i;
        for(i=1; i<=m; i++) {
            a[i].idx=i+a[i].rank;
            b[i].idx=i+b[i].rank;
        }
        
        Sensor[] c= new Sensor[2*m+1];
        for(i=1; i<=m; i++) c[i]=a[i];
        for(i=1; i<=m; i++) c[i+m]=b[i];
        
        int t;
        for(t=1; t<=2*m; t++) {
            // SEND
            for(i=1; i<=2*m; i++)
                if(c[i].idx==t) {
                    int[] message=new int[1];
                    message[0]=c[i].key;
                    c[i].send(message,channel);
                }
            
            if(channel.message==Channel.COLLISION ||channel.message==null) {
                System.out.println("COLLISION/null");
                for(i=1; i<=2*m; i++)
                    System.out.println
                    (i+" idx"+c[i].idx+" r"+c[i].rank+" g"+c[i].group+" k"+c[i].key);
            }
            
            // RECEIVE
            c[t].listen(channel);
            c[t].newkey= c[t].rcvdMsg[0];
            
            channel.clean();
            clock++;
        }
        
        for(i=1; i<=2*m; i++) c[i].key=c[i].newkey;
        
    }
    
    
    public void MergeSort1(Sensor[] s) {
        int m=s.length-1; // m is a power of two
        
        if(m>1) {
            Sensor[] c1=new Sensor[m/2+1];
            int i;
            for(i=1; i<= m/2; i++) c1[i]=s[i];
            MergeSort1(c1);
            
            Sensor[] c2=new Sensor[m/2+1];
            for(i=1; i<= m/2; i++) c2[i]=s[i+m/2];
            MergeSort1(c2);
            Merge1(c1,c2);
        }
    }
    
    
    
    
    // Rank2
    
    public void Rank2(Sensor[] a, Sensor[] b) {
        int m=a.length-1;
        int i;
        
        for(i=1; i<=m; i++) {
            a[i].group=1;
        }
        
        
        for(i=1; i<= (T.lStar(m)+1)/2+1; i++) {
            Regroup(2*i-1, a,b);
            Regroup(2*i, b, a);
        }
        
        
        int k,j;
        
        
        
        if(m>=2) {
            for(j=1; j<=m; j++) {
                b[j].rank=0;
            }
            
            for(i=1; i<=m; i++) {
                // SEND
                int[] message= new int[1];
                message[0]=a[i].key;
                a[i].send(message,channel);
                
                // RECEIVE
                for(j=1; j<=m; j++)
                    if(b[j].group==(i+1)/2) {
                        b[j].listen(channel);
                        if(b[j].key>b[j].rcvdMsg[0])
                            b[j].rank=i;
                    }
                
                channel.clean();
                clock++;
            }
            
            for(j=1; j<=m; j++) {
                a[j].rank=0;
            }
            
            for(i=1; i<=m; i++) {
                // SEND
                int[] message= new int[1];
                message[0]=b[i].key;
                b[i].send(message,channel);
                
                // RECEIVE
                for(j=1; j<=m; j++)
                    if(a[j].group==(i+1)/2) {
                        a[j].listen(channel);
                        if(a[j].key>a[j].rcvdMsg[0])
                            a[j].rank=i;
                    }
                
                channel.clean();
                clock++;
            }
            
        }
        
        
        
    }
    
   
    // Merging with energetic cost: O(\lg* m)
    
    public void Merge2(Sensor[] a, Sensor[] b) {
        int m=a.length-1;
        
        Rank2(a,b);
        
        int i;
        for(i=1; i<=m; i++) {
            a[i].idx=i+a[i].rank;
            b[i].idx=i+b[i].rank;
        }
        
        Sensor[] c= new Sensor[2*m+1];
        for(i=1; i<=m; i++) c[i]=a[i];
        for(i=1; i<=m; i++) c[i+m]=b[i];
        
        int t;
        for(t=1; t<=2*m; t++) {
            // SEND
            for(i=1; i<=2*m; i++)
                if(c[i].idx==t) {
                    int[] message=new int[1];
                    message[0]=c[i].key;
                    c[i].send(message, channel);
                }
            
            if(channel.message==Channel.COLLISION ||channel.message==null) {
                System.out.println("COLLISION/null");
                for(i=1; i<=2*m; i++)
                    System.out.println
                    (i+" "+c[i].idx+" "+c[i].rank+" "+c[i].key+" "+c[i].group);
            }
            
            // RECEIVE
            c[t].listen(channel);
            c[t].newkey= c[t].rcvdMsg[0];
            
            channel.clean();
            clock++;
        }
        
        for(i=1; i<=2*m; i++) c[i].key=c[i].newkey;
        
    }
    
    
    public void MergeSort2(Sensor[] s) {
        int m=s.length-1; // m is a power of two
        
        if(m>1) {
            Sensor[] c1=new Sensor[m/2+1];
            int i;
            for(i=1; i<= m/2; i++) c1[i]=s[i];
            MergeSort2(c1);
            
            Sensor[] c2=new Sensor[m/2+1];
            for(i=1; i<= m/2; i++) c2[i]=s[i+m/2];
            MergeSort2(c2);
            Merge2(c1,c2);
        }
    }
    
    
    
    
    /// AUXILIARY METHODS
    
    
    // Generating sequence of random pairwise distinct keys
    
    public void randomKeys(Sensor[] s) {
        Random random=new Random(System.currentTimeMillis());
        int n=s.length-1;
        int i;
        for(i=1; i<=n; i++)
            s[i].key=(n+1)*random.nextInt(10000)+i; // distinct keys
        
    }
    
    
    public void randomSortedKeys(Sensor[] s, int suffix)
    // Suffix (0 or 1) can be used to ensure that all keys in one sequence are
    // even and in the other are odd
    {
        Random random=new Random(System.currentTimeMillis());
        int n=s.length-1;
        int i;
        s[1].key=2*random.nextInt(10000)+suffix;
        for(i=2; i<=n; i++)
            s[i].key= s[i-1].key+2*random.nextInt(10000)+2; // distinct keys
        
    }
    
    
    public long sumKeys(Sensor[] s) {
        long x=0;
        int n=s.length-1;
        int i;
        for(i=1; i<=n; i++) x+=s[i].key;
        return x;
    }
    
    public boolean sortedKeys(Sensor[] s) {
        boolean x=true;
        int n=s.length-1;
        int i;
        for(i=1; x && i<=n-1; i++) x= x && (s[i].key<s[i+1].key);
        return x;
    }
    
    public int maxSE(Sensor[] s) {
        int n=s.length-1;
        int i, se=-1;
        for(i=1; i<=n; i++) if(s[i].se>se) se=s[i].se;
        return se;
    }
    
    public int maxLE(Sensor[] s) {
        int n=s.length-1;
        int i, le=-1;
        for(i=1; i<=n; i++) if(s[i].le>le) le=s[i].le;
        return le;
    }
    
    
    // main() METHODS    

    
    /* Uncomment the procedure below if you want to simulate only merging
     */

    /*     
    public static void main(String[] args)
    // simulating merging
    {
     
        // here you can change the data size
        int n=T.power(2,11); 
      
        RadioNetwork RN=new RadioNetwork(2*n);
     
        System.out.println("MERGING. Data size: "+(RN.sensor.length-1));
        System.out.println("n : "+n);
     
        Sensor[] a=new Sensor[n+1];
        Sensor[] b=new Sensor[n+1];
        int i;
        for(i=1; i<=n; i++) a[i]=RN.sensor[i];
        for(i=1; i<=n; i++) b[i]=RN.sensor[n+i];
     
     
        RN.randomSortedKeys(a,0);
        RN.randomSortedKeys(b,1);
	
        System.out.println("sum ="+ RN.sumKeys(RN.sensor));
        System.out.println("sorted: "+ RN.sortedKeys(RN.sensor));

        System.out.println("merging ...");
    
 
        //  Uncomment one of the three procedures below
     
        //  RN.Merge(a,b);
        // RN.Merge1(a,b);
      	RN.Merge2(a,b);
     
        System.out.println("sum ="+ RN.sumKeys(RN.sensor));
        System.out.println("sorted: "+ RN.sortedKeys(RN.sensor));
        System.out.println("clock: "+RN.clock);
        System.out.println("energetic cost of listening: "+RN.maxLE(RN.sensor));
        System.out.println("energetic cost of sending: "+RN.maxSE(RN.sensor));
        System.out.println("energetic cost: "+(RN.maxSE(RN.sensor)+RN.maxLE(RN.sensor))); 
         
    }
    */ 
    
    
    
    
    /* Uncomment procedure below if you want to simulate merge-sort
     */ 
    
    
    
    public static void main(String[] args)
    // simulating merge-sort
    {
	// here you can change data size 
	// n should be power of two; use the function T.power(2, ... )
	int n=T.power(2, 11);


	RadioNetwork RN=new RadioNetwork(n);
        
        System.out.println("SORTING. Data size: "+(RN.sensor.length-1));
        
        RN.randomKeys(RN.sensor);
        System.out.println("sum ="+ RN.sumKeys(RN.sensor));
        System.out.println("sorted: "+ RN.sortedKeys(RN.sensor));
        
        System.out.println("sorting ...");

        //  Uncomment one of the three procedures below
        
        RN.MergeSort(RN.sensor);
        //  RN.MergeSort1(RN.sensor);
        //  RN.MergeSort2(RN.sensor);
        
        System.out.println("sum ="+ RN.sumKeys(RN.sensor));
        System.out.println("sorted: "+ RN.sortedKeys(RN.sensor));
        System.out.println("clock: "+RN.clock);
        System.out.println("energetic cost of listening: "+RN.maxLE(RN.sensor));
        System.out.println("energetic cost of sending: "+RN.maxSE(RN.sensor));
        System.out.println("energetic cost: "+(RN.maxSE(RN.sensor)+RN.maxLE(RN.sensor))); 
        
    }
    
    
    
}


class Sensor
// sensor variables as described in the paper
{
    
    
    int key;
    int rank;
    int group;
    
    // key', rank', group'
    int key1;
    int rank1;
    int group1;
    
    // key''
    int key2;
    
    int timer;
    int idx;
    int newkey;
    boolean winner;
    
    // ENERGY used
    
    int le; // listening
    int se; // sending
    
    // Universal methods
    
    int[] rcvdMsg;
    
    public void send(int[] message, Channel channel) {
        channel.insert(message);
        se++;
    }
    
    public void listen(Channel channel) {
        rcvdMsg=channel.message;
        le++;
    }
    
    
}


class Channel {
    
    public static final int[] COLLISION={};
    
    int[] message; // message is a tuple of integers
    
    public void insert(int[] m) {
        if (message== null) message=m;
        else message= COLLISION;
    }
    
    public void clean() {
        message=null;
    }
    
}


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
            }
            else // x>x1
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
            }
            else // x>x1
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
            }
            else // x>x1
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
    
    public static int alpha(int m, int i, int j, int k) {
        return  (j-1)*T.h(m,i)+k;
    }
    
    
    //// Time estimations
    
    public static int RankTime(int m) {
        return m;
    }
    
    public static int MergeTime(int m) {
        return 4*m;
        
        // 2*RankTime(m)+2*m
    }
    
    public static int RegroupTime(int m, int i) {
        return g(m,i-1)*h(m,i-1)+g(m,i)*h(m,i)+2*m-1;
        
        // Phase 1: g(m,i-1)*h(m,i-1)+g(m,i)*(h(m,i)-1)
        // Phase 2: g(m,i)
        // Phase 3: m
        // Phase 4: m-1
        
    }
    
    public static int Rank1Time(int m) {
        return 2*g(m,1)*h(m,1)+3*m-1;
        
        // RegroupTime(m,1): 1*m+g(m,1)*h(m,1)+2m-1
        // Phase B:          g(m,1)*h(m,1)
    }
    
    
    public static int Merge1Time(int m) {
        return 4*g(m,1)*h(m,1)+8*m-2;
        
        // 2*Rank1Time: 4*g(m,1)*h(m,1)+6*m-2;
        // Permutation routing: 2*m
    }
    
    
    public static int Rank2Time(int m) {
        int s=0, i;
        
        // iterated regrouping
        
        for(i=1; i<= (lStar(m)+1)/2+1; i++)
            s+=RegroupTime(m, 2*i-1)+RegroupTime(m, 2*i);
        
        
        return s+2*m;
        // last ranking of a in b and of b in a: 2*m
        
    }
    
    
    public static int Merge2Time(int m) {
        return Rank2Time(m)+2*m;
        // permutation routing: 2*m
    }
    
    
    //// Estimations of energetic cost of sending
    
    
    public static int RankSE(int m) {
        return 1;
    }
    
    public static int MergeSE(int m) {
        return 2;
    }
    
    
    
    public static int RegroupSEd(int m,int i) {
        
        int d1= 1;
        int d12=(m%h(m,i) >0)? 1:0;
        int d3=0;
        int d4=1;
        
        // System.out.println("d1="+d1+" d2="+d2);
        
        return d1+d12+d3+d4;
        
        // Phase 1
        // Phase 2
        // Phase 3
        // Phase 4
    }
    
    
    public static int RegroupSEc(int m,int i) {
        
        int c1=1;
        int c2=1;
        int c3=1;
        int c4=0;
        
        return c1+c2+c3+c4;
        
        // Phase 1
        // Phase 2
        // Phase 3
        // Phase 4
    }
    
    
    
    public static int Rank1SEa(int m) {
        return RegroupSEd(m,1);
        // Regroup(b,a)
        // Phase B
        
    }
    
    
    public static int Rank1SEb(int m) {
        return RegroupSEc(m,1)+1 ;
        // Regroup(b,a)
        // Phase B
    }
    
    
    
    
    
    public static int Merge1SE(int m) {
        return Rank1SEa(m)+Rank1SEb(m)+1;
        // Rank1(a,b)
        // Rank1(b,a)
        // Permutation routing: +1
    }
    
    
    /////////
    
    public static int Rank2SE(int m) {
        int sa=0, sb=0, i;
        
        for(i=1; i<=(lStar(m)+1)/2+1; i++) {
            sa+=RegroupSEc(m,2*i-1)+RegroupSEd(m,2*i);
            sb+=RegroupSEd(m,2*i-1)+RegroupSEc(m,2*i);
        }
        
        return Math.max(sa,sb)+1;
        // iterated Regroup
        // last ranking a in b and b in a: +1
    }
    
    public static int Merge2SE(int m) {
        return Rank2SE(m)+1;
        // Rank2;
        // permutation routing: 1
    }
    
    
    //// Estimations of energetic cost of listening    
    
    public static int RankLE(int m) {
        return h(m,1);
    }
    
    public static int MergeLE(int m) {
        return h(m,1)+1;
        // Rank(a,b):  a: h(m,1)  b:0
        // Rank(b,a):  a: 0       b: h(m,1)
        // Permutation routing: 1
    }
    
    
    public static int RegroupLEd(int m, int i) {
        int d1= (m%h(m,i)>0)? 2 : 0 ;
        int d2=0;
        int d3=1;
        int d4=1;
        
        return d1+d2+d3+d4;
        // Phase 1:  d: 0+(2)   c: 2
        // Phase 2:  d: 0       c: 2
        // Phase 3:  d: 1       c: 0
        // Phase 4:  d: 1       c: 0
    }
    
    ////
    
    public static int RegroupLEc(int m, int i) {
        
        int c12=3; // splitter: 1+2 slave: 2+0
        int c3=0;
        int c4=0;
        
        return c12+c3+c4;
        // Phase 1:
        // Phase 2:
        // Phase 3:
        // Phase 4:
    }
    
    
    /////
    
    public static int Rank1LEa(int m) {
        return RegroupLEd(m,1)+h(m,2);
        // Regroup(b,a):
        // Phase B:      a: h(2,m) b: 0
    }
    
    public static int Rank1LEb(int m) {
        return RegroupLEc(m,1);
        // Regroup(b,a):
        // Phase B:      a: h(2,m) b: 0
    }
    
    
    
    
    public static int Merge1LE(int m) {
        return Rank1LEa(m)+Rank1LEb(m)+1;
        // Rank1(a,b):
        // Rank1(b,a):
        // perm. rout.: +1
        
    }
    
    
    ////
    
    
    public static int Rank2LE(int m) {
        int sa=0, sb=0, i;
        
        for(i=1; i<=(lStar(m)+1)/2+1; i++) {
            sa+=RegroupLEc(m,2*i-1)+RegroupLEd(m,2*i);
            sb+=RegroupLEd(m,2*i-1)+RegroupLEc(m,2*i);
        }
        
        return Math.max(sa,sb)+2;
        // iterated Regroup
        // last ranking a in b and b in a: +2
    }
    
    
    
    public static int Merge2LE(int m) {
        return Rank2LE(m)+1;
        // Rank2(a,b):
        // Perm. rout.: +1
        
    }
    
};



