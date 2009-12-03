package org.getopt.luke;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Lucene" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Lucene", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import org.apache.lucene.util.PriorityQueue;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;

import java.io.File;
import java.util.Hashtable;

/**
 * <code>HighFreqTerms</code> class extracts terms and their frequencies out
 * of an existing Lucene index.
 *
 * @version $Id: HighFreqTerms.java,v 1.2 2003/11/08 10:55:40 Administrator Exp $
 */
public class HighFreqTerms {
    public static int defaultNumTerms = 100;
    
    public static void main(String[] args) throws Exception {
        Directory dir = FSDirectory.open(new File(args[0]));
        TermInfo[] terms = getHighFreqTerms(IndexReader.open(dir), null, new String[]{"body"});
        for (int i = 0; i < terms.length; i++) {
            System.out.println(i + ".\t" + terms[i].term);
        }
    }
    
    public static TermInfo[] getHighFreqTerms(IndexReader ir, Hashtable junkWords, String[] fields) throws Exception {
        return getHighFreqTerms(ir, junkWords, defaultNumTerms, fields);
    }
    
    public static TermInfo[] getHighFreqTerms(IndexReader reader, Hashtable junkWords, int numTerms, String[] fields) throws Exception {
        if (reader == null || fields == null) return null;
        TermInfoQueue tiq = new TermInfoQueue(numTerms);
        TermEnum terms = reader.terms();
        
        int minFreq = 0;
        while (terms.next()) {
            String field = terms.term().field();
            if (fields != null && fields.length > 0) {
                boolean skip = true;
                for (int i = 0; i < fields.length; i++) {
                    if (field.equals(fields[i])) {
                        skip = false;
                        break;
                    }
                }
                if (skip) continue;
            }
            if (junkWords != null && junkWords.get(terms.term().text()) != null) continue;
            if (terms.docFreq() > minFreq) {
                tiq.insertWithOverflow(new TermInfo(terms.term(), terms.docFreq()));
                if (tiq.size() >= numTerms) 		     // if tiq overfull
                {
                    tiq.pop();				     // remove lowest in tiq
                    minFreq = ((TermInfo)tiq.top()).docFreq; // reset minFreq
                }
            }
        }
        TermInfo[] res = new TermInfo[tiq.size()];
        for (int i = 0; i < res.length; i++) {
            res[res.length - i - 1] = (TermInfo)tiq.pop();
        }
        return res;
    }
}

final class TermInfoQueue extends PriorityQueue<TermInfo> {
    TermInfoQueue(int size) {
        initialize(size);
    }
    protected final boolean lessThan(TermInfo a, TermInfo b) {
        TermInfo termInfoB = (TermInfo)b;
        return a.docFreq < b.docFreq;
    }
}
