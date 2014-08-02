
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.it.ItalianLightStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.ElisionFilter;
import org.apache.lucene.util.Version;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.util.plugin.SolrCoreAware;




public class HierarchyComponent extends SearchComponent implements SolrCoreAware {
	
	//hashmap dei livelli gerarchici <Gerarchia,Count>
	private static HashMap<String,Integer> h =new HashMap<String, Integer>();
	
	public void inform(SolrCore arg0) {
	// TODO Auto-generated method stub
	}
	@Override
	public String getDescription() {
	// TODO Auto-generated method stub
	return null;
	}
	@Override
	public String getSource() {
	// TODO Auto-generated method stub
	return null;
	}
	@Override
	public void prepare(ResponseBuilder arg0) throws IOException {
	// TODO Auto-generated method stub
	}
	@SuppressWarnings("unchecked")
	
	@Override
	public void process(ResponseBuilder rb) throws IOException {
			/*
			* Dalla risposta di Solr recuperiamo i cluster generati.
			*/
			ArrayList<?> cluster = (ArrayList<?>) rb.rsp.getValues().get("clusters");
			// inizzializzo l'arrayList di cluster
			ArrayList<Cluster> clusters = new ArrayList<Cluster>();
			// ciclo sui cluster
			String queryThes="";
			for (int j=0; j<cluster.size();j++)
			{
				SimpleOrderedMap<Object> map = (SimpleOrderedMap<Object>) cluster.get(j);
				// recupero la label del cluster
				String l = map.get("labels").toString();
				// elimino il primo e l'ultimo carattere della label
				l = l.substring(1,l.length()-1);
				// richiamo la funzione che si occuperà di effettuare lo stemming italiano della label corrente
				String stemLabel = italianAnalyzer(l);
				
				if(!stemLabel.equals("other topics"))
    	    	{	
    	    			if(j==0)
		    	    		queryThes+="descrittore:\""+stemLabel+"\"";
		    	    	else
		    	    		queryThes+=" OR descrittore:\""+stemLabel+"\""; 
    	    	}
				
				// recupero gli id dei documenti presenti nel cluster
				String docs = map.get("docs").toString().substring(1,map.get("docs").toString().length()-1);
				// creo un arrayList con gli id dei documenti
				String[] s = docs.split(",");
				ArrayList<String> docList = new ArrayList<String>(Arrays.asList(s));
				// creo un oggetto della classe Cluster settando i parametri "label" "stemLabel" "docs"
				Cluster c = new Cluster(l, stemLabel, docList);
				// aggiungo alla lista il cluster appena creato in maniera ordinata
				clusters.add(getSortedIndex(stemLabel,clusters), c);
			}
			
			
			SolrServer server = new HttpSolrServer("http://localhost:8983/solr/thesaurus");
    	    SolrQuery query = new SolrQuery();
    	    
    	    query.setQuery(queryThes);
    	    query.setFacet(true);
    	    query.setFacetMinCount(1);
    	    query.addFacetField("hierarchy");
    	    query.addSort("descrittore",ORDER.asc);
    	    query.setFacetSort("index");
    	    
    	    
    	    QueryResponse response2=new QueryResponse();
    	    
			try {
				response2 = server.query(query);
			} catch (SolrServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
    	    SolrDocumentList results=response2.getResults();
    	    
    	    ArrayList<Desc> descrittori=new ArrayList<Desc>();
    	    
    	    for(int i=0;i<results.size();i++)
    	    {
    	    	Desc aux=new Desc();
    	    	aux.setName(results.get(i).getFieldValue("descrittore").toString());
    	    	aux.setGerarchia((ArrayList)(results.get(i).getFieldValues("hierarchy")));
    	    	descrittori.add(aux);
    	    }    	    
    	    
    	    ArrayList<Desc> out=merge (clusters,descrittori); 	    
    	    
    	    for(int i=0; i<out.size();i++)
    	    {
    	    	
    	    	System.out.println(out.get(i).getGerarchia().get(0));
    	    	System.out.println(out.get(i).getName());
    	    	for(int j=0; j<out.get(i).getDocs().size();j++)
    	    	{
    	    		System.out.println(out.get(i).getDocs().get(j));
    	    	}    	    	
    	    }
    	    
    	    printMap(h);
    	    
	}
	
	/*
	* Funzione per lo stemming "text_it" di una stringa
	*/
	private static String italianAnalyzer(String label) throws IOException
	{
		// recupero le stopWords dell'ItalianAnalyzer
		CharArraySet stopWords = ItalianAnalyzer.getDefaultStopSet();
		// andiamo a tokenizzare la label
		TokenStream tokenStream = new StandardTokenizer(Version.LUCENE_48, new StringReader(label));
		// richiamo l'Elision Filter che si occupa di elidere le lettere apostrofate
		tokenStream = new ElisionFilter(tokenStream, stopWords);
		// richiamo il LowerCaseFilter
		tokenStream = new LowerCaseFilter(Version.LUCENE_48, tokenStream);
		// richiamo lo StopFilter per togliere le stop word
		tokenStream = new StopFilter(Version.LUCENE_48, tokenStream, stopWords);
		// eseguo il processo di stemming italiano per ogni token
		tokenStream = new ItalianLightStemFilter(tokenStream);
		/*
		* ricostruisco la stringa in precedenza tokenizzata
		*/
		StringBuilder sb = new StringBuilder();
		CharTermAttribute charTermAttribute =tokenStream.addAttribute(CharTermAttribute.class);
		tokenStream.reset();
		while (tokenStream.incrementToken())
		{
		String term = charTermAttribute.toString();
		sb.append(term + " ");
		}
		tokenStream.close();
		// elimino l'ultimo carattere (spazio vuoto)
		String l = sb.toString().substring(0,sb.toString().length()-1);
		// ritorno la label stemmata
		return l;
	}
	
	/*
	* Funzione per l'inserimento in maniera ordinata in una lista
	*/
	private static int getSortedIndex (String name, ArrayList<Cluster> list) {
	for (int i=0; i < list.size(); i++) {
	if (name.compareTo(list.get(i).getStemLabel()) < 0) {
	return i;
	}
	}
	return list.size();
	}
	

private static ArrayList<Desc> merge (ArrayList<Cluster> clusters,ArrayList<Desc> descrittori) {
		  
		  ArrayList<Desc> output=new ArrayList<Desc>();
		  int indexC=0;
		  int indexD=0;
		  
		  while(indexD<descrittori.size())
		  {
			  if(clusters.get(indexC).getStemLabel().equals(descrittori.get(indexD).getName()))
			  {
				  descrittori.get(indexD).setDocs(clusters.get(indexC).getDocs());
				  descrittori.get(indexD).setName(clusters.get(indexC).getlabel());
				  output.add(descrittori.get(indexD));
				  // qui vado a richiamare la funzione per la costruzione del primo livello	 
				  buildFacetLevel(descrittori.get(indexD));
				 
				  
				  
				  indexC++;
				  indexD++;
			  }
			  else{  
				  Desc aux= new Desc();
				  aux.setDocs(clusters.get(indexC).getDocs());
				  aux.setName(clusters.get(indexC).getLabel());
				  aux.setGerarchia(new ArrayList<String>(Arrays.asList(clusters.get(indexC).getLabel())));
				  output.add(aux);
				  buildFacetLevel(aux);
				  indexC++;
			  }  
		  }
		  while(indexC<clusters.size())
		  {
			  Desc aux= new Desc();
			  aux.setDocs(clusters.get(indexC).getDocs());
			  aux.setName(clusters.get(indexC).getLabel());
			  aux.setGerarchia(new ArrayList<String>(Arrays.asList(clusters.get(indexC).getLabel())));
			  output.add(aux);
			  buildFacetLevel(aux);
			  indexC++;
		  }
		  return output;
		  
	  }


private static void buildFacetLevel(Desc descrittore)
{
	 for(int k=0; k<descrittore.getGerarchia().size();k++)
	  {
		  String[] first=  descrittore.getGerarchia().get(k).split("/");
		  
		// se non esiste il primo livello, allora vado a inserire esso ed il count 
			if (!h.containsKey(first[0]))
			{
				h.put(first[0], descrittore.getDocs().size());
			}
			//altrimenti aggiorno il count
			else
			{
				h.put(first[0], h.get(first[0]) + descrittore.getDocs().size());
			}
	  
	  }
}

//metodo che mi stampa solo il contenuto della hashmap
public static void printMap(Map mp) {
    Iterator it = mp.entrySet().iterator();
    while (it.hasNext()) {
        Map.Entry pairs = (Map.Entry)it.next();
        System.out.println(pairs.getKey() + " = " + pairs.getValue());
        it.remove(); // avoids a ConcurrentModificationException
    }
}

}