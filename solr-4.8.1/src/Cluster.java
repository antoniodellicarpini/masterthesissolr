


import java.util.ArrayList;

public class Cluster {
	
	private String label;
	private String stemLabel;
	private ArrayList<String> docs=new ArrayList<String>();
	
	
	public Cluster(String label,String stemLabel, ArrayList<String> docs)
	{
		
		this.setDocs(docs);
		this.setlabel(label);
		this.setStemLabel(stemLabel);
	}
	
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getStemLabel() {
		return stemLabel;
	}

	public void setStemLabel(String stemLabel) {
		this.stemLabel = stemLabel;
	}

	public String getlabel() {
		return label;
	}
	public void setlabel(String label) {
		this.label = label;
	}
	public ArrayList<String> getDocs() {
		return docs;
	}
	public void setDocs(ArrayList<String> docs) {
		this.docs = docs;
	}
	
}
