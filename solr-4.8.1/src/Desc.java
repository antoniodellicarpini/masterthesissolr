


import java.util.ArrayList;

public class Desc {
 
	String name;
	ArrayList<String> gerarchia=new ArrayList<String>();
	ArrayList<String> docs=new ArrayList<String>();
	public Desc()
	{
	  
	}
	public Desc(String name, ArrayList<String> gerarchia)
	{
	 setName(name);
	 setGerarchia(gerarchia);
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public ArrayList<String> getGerarchia() {
		return gerarchia;
	}
	public void setGerarchia(ArrayList<String> gerarchia) {
		this.gerarchia = gerarchia;
	}
	public ArrayList<String> getDocs() {
		return docs;
	}
	public void setDocs(ArrayList<String> docs) {
		this.docs = docs;
	}
	
	
}
