import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

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
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.util.plugin.SolrCoreAware;


public class HierarchyComponent extends SearchComponent implements SolrCoreAware {
	//hashmap dei livelli gerarchici <Gerarchia,Count>
	private  HashMap<String,ArrayList<String>> h = new HashMap<String, ArrayList<String>>();
	
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
		h.clear();
		ArrayList<?> cluster;
		
		 if(rb.req.getParams().get("clicked")!=null){
			 
			 String first=rb.req.getParams().get("first");
			 
			 SolrServer server = new HttpSolrServer("http://localhost:8983/solr/corpus");
			 	
			    SolrQuery query = new SolrQuery();
			    query.set("q", first);
			    query.set("qt", "/clustering");
			    /*
			     * Effettuiamo la query sul server e recuperiamo tutti i documenti ottenuti in output.
				*/
			    QueryResponse response = null;
				try {
					response = server.query(query);
				} catch (SolrServerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			    
				cluster= (ArrayList) response.getResponse().get("clusters");
				
			 
		 }
		 
		 else{
		/*
		 * Dalla risposta di Solr recuperiamo i cluster generati. 
		 */
		   cluster = (ArrayList) rb.rsp.getValues().get("clusters");
		// inizzializzo l'arrayList di cluster
	     
	    // ciclo sui cluster
		 }
	    
		String queryThes="";
		ArrayList<Cluster> clusters = new ArrayList<Cluster>();
		for (int j=0; j<cluster.size();j++)
		{
			SimpleOrderedMap<Object> map = (SimpleOrderedMap<Object>) cluster.get(j);
			// recupero la label del cluster
			String l = map.get("labels").toString();
			// elimino il primo e l'ultimo carattere della label
			l = l.substring(1,l.length()-1);
			// richiamo la funzione che si occuperà di effettuare lo stemming italiano della label corrente
			String stemLabel = italianAnalyzer(l);
			// recupero gli id dei documenti presenti nel cluster
			String docs = map.get("docs").toString().substring(1,map.get("docs").toString().length()-1);    	    	
	    	// creo un arrayList con gli id dei documenti
	    	String[] s = docs.split(",");
	    	ArrayList<String> docList = new ArrayList<String>(Arrays.asList(s));
	    	// creo un oggetto della classe Cluster settando i parametri "label" "stemLabel" "docs"
	    	Cluster c = new Cluster(l, stemLabel, docList);     	    	
	    	// aggiungo alla lista il cluster appena creato in maniera ordinata
	    	clusters.add(getSortedIndex(stemLabel,clusters), c);
			
	    	/*
	    	 *  costruisco la query da effettuare sul thesaurus
	    	 */
	    	
	    	if(!stemLabel.equals("other topics"))
	    	{	
	    			if(j==0)
	    	    		queryThes+="descrittore:\""+stemLabel+"\"";
	    	    	else
	    	    		queryThes+=" OR descrittore:\""+stemLabel+"\""; 
	    	}
		}
		
		
		
		/*
	     * Setting dei parametri di connessione al core "thesaurus" per una query con
	     * il componente di Facet attivato sul field "hierarchy". Richiediamo inoltre che
	     * i risultati siano ordinati per "descrittore".
		*/
		
		SolrServer server = new HttpSolrServer("http://localhost:8983/solr/thesaurus");
	    SolrQuery query = new SolrQuery();
	    
	    query.setQuery(queryThes);
	    query.setFacet(true);
	    query.setFacetMinCount(1);
	    query.addFacetField("hierarchy");
	    query.addSort("descrittore",ORDER.asc);
	    query.setFacetSort("index");
	    /*
	     * Effettuiamo la query sul server e recuperiamo tutti i documenti ottenuti in output.
		*/
	    
	    QueryResponse response = null;
		try {
			response = server.query(query);
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    SolrDocumentList results = response.getResults();
	    
	    // inizializziamo l'arrayList di descrittori
	    ArrayList<Desc> descrittori=new ArrayList<Desc>();
	    
	    // ciclo sui documenti
	    for(int i=0;i<results.size();i++)
	    {
	    	// per ogni documento creo un oggetto della classe "Desc"
	    	Desc descrittore = new Desc();
	    	// recupero il descrittore e setto tale valore nell'oggetto creato
	    	descrittore.setName(results.get(i).getFieldValue("descrittore").toString());
	    	// recupero le gerarchie e setto tale valore nell'oggetto creato
	    	descrittore.setGerarchia((ArrayList)(results.get(i).getFieldValues("hierarchy")));
	    	// aggiunto l'oggetto alla lista dei descrittori
	    	descrittori.add(descrittore);
	    }
	    
	    
	    /*
	     * Richiamo della funzione "merge" che effettuerà il confronto tra la lista
	     * dei cluster e la lista dei descrittori generando in output un'unica lista
	     * contenente tutti i termini ognuno dei quali avrà associato i propri documenti.
		*/
	    ArrayList<Desc> out = merge (clusters,descrittori);
	    
	    if(rb.req.getParams().get("clicked")!=null){
	    	
	    	HashMap<String,ArrayList<String>> elements = new HashMap<String, ArrayList<String>>();
	        ArrayList<String> docs = new ArrayList<String>();
	         int i=0;
	         // cicliamo sui descrittori
	         while(i < out.size()){
	          boolean trovato=false;
	          int j=0;
	          // cicliamo sulle gerarchie di un descrittore
	          while(j < out.get(i).getGerarchia().size() && !trovato){
	           String[] parole =  out.get(i).getGerarchia().get(j).split("/");
	           
	          
	           int k=0;
	           while(k < parole.length && !trovato){
	           
	            //rb.req.getParams().get("clicked").substring(1, rb.req.getParams().get("clicked").length()-1)
	            if(parole[k].equals(rb.req.getParams().get("clicked"))){
	             trovato=true;
	            
	            } 
	           k++;
	           }
	           
	           if(trovato){
	            if(k==parole.length){
	            
	             //prendiamo solo i docs
	             docs = out.get(i).getDocs(); 
	            }
	            else{
	            
	             //prendiamo l'elemento figlio dell'elemento in posizione "k"
	             
	             if (!elements.containsKey(parole[k]))
	             {
	            	 ArrayList<String> a=new ArrayList<String>();
	              
	            	 for(int z =0;z<out.get(i).getDocs().size();z++)
						{
	            		 a.add(out.get(i).getDocs().get(z).replaceAll("\\s+",""));
	            		
						}
	               
	            	 elements.put(parole[k], a);
	              
	             }
	             //altrimenti aggiorno il count
	             else
	             {
	            	 
	            	 for(int z =0;z<out.get(i).getDocs().size();z++)
						{
							
	            		 	
							elements.get(parole[k]).add(out.get(i).getDocs().get(z).replaceAll("\\s+",""));
							
						}
	            	 
	              
	              
	             }
	             
	             
	            }
	           
	           }
	          j++;
	          }
	         i++;
	         }
	         
	        // aggiunta alla response
	         
	         Iterator it = elements.entrySet().iterator();
	         
	         NamedList[] array = new NamedList[elements.size()];
	         
	         int contatore=0;
	         int index=0;
	         while (it.hasNext()) {
	          
	          NamedList embeddedResponse = new SimpleOrderedMap();
	             HashMap.Entry resp = (HashMap.Entry)it.next();
	             String[] myStringArray = new String[1];
	 	         myStringArray[0] = (String) resp.getKey();
	 	         embeddedResponse.add("labels", myStringArray);
	             //embeddedResponse.add("labels", (String) resp.getKey());
	             embeddedResponse.add("numDocs", ((ArrayList)resp.getValue()).size());
	             contatore=contatore+((ArrayList)resp.getValue()).size();
	             embeddedResponse.add("idDocs", resp.getValue());
	             array[index] = embeddedResponse;
	             index++;
	             
	         }
	         
	         contatore=contatore+docs.size();
	         
	         if(contatore==0) // caso in cui abbiamo cliccato un documento
	        	 rb.rsp.add("clicked", rb.req.getParams().get("clicked"));
	         else
	        	 rb.rsp.add("clicked", rb.req.getParams().get("clicked")+" ("+contatore+")");
	         
	         rb.rsp.add("clusterGerarchizzati",array);
	         rb.rsp.add("documents", docs);
	         rb.rsp.add("first", rb.req.getParams().get("first"));
	    	
	    	
	    	
	    	
	    
	    	//***********************************************
	    }
	    else{
	    
	    
	    Iterator it = h.entrySet().iterator();
	    
	    NamedList[] array = new NamedList[h.size()];
	    
	    int i=0;
	    while (it.hasNext()) {
	    	
	    	NamedList embeddedResponse = new SimpleOrderedMap();
	        HashMap.Entry resp = (HashMap.Entry)it.next();
	        String[] myStringArray = new String[1];
	        myStringArray[0] = (String) resp.getKey();
	        embeddedResponse.add("labels", myStringArray);
	        embeddedResponse.add("numDocs", ((ArrayList)resp.getValue()).size());
	        embeddedResponse.add("idDocs", resp.getValue());
	       
	        array[i] = embeddedResponse;
	        i++;
	        
	    }
	    
	    rb.rsp.add("clusterGerarchizzati",array);
	    rb.rsp.add("clicked","");
	    // la prima richiesta
	    if(rb.req.getParams().get("q")!=null)
	    	rb.rsp.add("first", rb.req.getParams().get("q"));
	    else
	    	rb.rsp.add("first", "*:*");
	    
	    }
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
	    CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
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
	 *  Funzione per l'inserimento in maniera ordinata in una lista
	 */
	
	private static int getSortedIndex (String name, ArrayList<Cluster> list) {
        for (int i=0; i < list.size(); i++) {
            if (name.compareTo(list.get(i).getStemLabel()) < 0) {
                return i;
            }
        }       
    return list.size();
   }
	
	
	
	
	private  ArrayList<Desc> merge (ArrayList<Cluster> clusters,ArrayList<Desc> descrittori) {
		  
		  ArrayList<Desc> output = new ArrayList<Desc>();
		  
		  int indexC = 0;
		  int indexD = 0;
		  // finchè non scandisco tutti i descrittori eseguo:
		  while(indexD < descrittori.size())
		  {
			  /* 
			   * se il confronto va a buon fine, cioè se la label del descrittore 
			   * è uguale a quello del cluster.
			   */
			  if(clusters.get(indexC).getStemLabel().equals(descrittori.get(indexD).getName()) )
			  {
				  // prendo i documenti associati al cluster e li setto al descrittore.
				  descrittori.get(indexD).setDocs(clusters.get(indexC).getDocs());
				  // recupero inoltre la label non stemmata del cluster
				  descrittori.get(indexD).setName(clusters.get(indexC).getlabel());
				  // aggiungo all'output il descrittore
				  output.add(descrittori.get(indexD));
				  
				  // richiamo la funzione per la costruzione del primo livello	 
				  buildFacetLevel(descrittori.get(indexD));
				  
				  // avanzo entrambi gli indici delle due liste
				  indexC++;
				  indexD++;
			  }
			  /* 
			   * se il confronto non va a buon fine, cioè se la label del descrittore 
			   * è diversa da quella del cluster, allora tale cluster non è stato trovato
			   * nel thesaurus e quindi posso aggiungerlo direttamente all'output
			   */
			  else
			  {
				  
				  // creo un nuovo oggetto descrittore e gli setto i documenti, la label e la gerarchia.
				  Desc descrittore = new Desc();
				  descrittore.setDocs(clusters.get(indexC).getDocs());
				  descrittore.setName(clusters.get(indexC).getLabel());
				  descrittore.setGerarchia(new ArrayList<String>(Arrays.asList(clusters.get(indexC).getLabel())));
				  // aggiungo l'oggetto all'output
				  output.add(descrittore);
				  // richiamo la funzione per la costruzione del primo livello	 
				  buildFacetLevel(descrittore);
				  // incremento l'indice della lista di cluster
				  indexC++;
			  }
			  
		  }
		  
		  // terminata la scansione dei descrittori rimangono dei cluster da inserire nell'output
		  while(indexC < clusters.size())
		  {
			  Desc descrittore = new Desc();
			  descrittore.setDocs(clusters.get(indexC).getDocs());
			  descrittore.setName(clusters.get(indexC).getLabel());
			  descrittore.setGerarchia(new ArrayList<String>(Arrays.asList(clusters.get(indexC).getLabel())));
			  output.add(descrittore);
			  buildFacetLevel(descrittore);
			  indexC++;
		  }
		  
		  return output;
	}
	
	private  void buildFacetLevel(Desc descrittore)
	{
		int size = descrittore.getGerarchia().size();
		ArrayList<String> a;
	
		HashSet oldAux=new HashSet(); 
		
		for(int k=0; k < size; k++)
		{
			
			  	String[] first =  descrittore.getGerarchia().get(k).split("/");
			  	
			  	if(oldAux.add(first[0]))
			  	{
			  	// se non esiste il primo livello, allora vado a inserire esso ed il count 
					if (!h.containsKey(first[0]))
					{ 
						a=new ArrayList<String>();
						for(int i =0;i<descrittore.getDocs().size();i++)
								{
									a.add(descrittore.getDocs().get(i).replaceAll("\\s+",""));

								}
						h.put(first[0],a);
					}
					//altrimenti aggiorno il count
					else
					{
						for(int i =0;i<descrittore.getDocs().size();i++)
						{
							
							h.get(first[0]).add(descrittore.getDocs().get(i).replaceAll("\\s+",""));
							
						}
						
					}

			  	}
			  	
			  	
		 }
	}
	
}
