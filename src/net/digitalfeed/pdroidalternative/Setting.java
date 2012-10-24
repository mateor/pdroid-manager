package net.digitalfeed.pdroidalternative;

class Setting implements Comparable<Setting> {
	private String id;
	private String name;
	private String group;
	private String label;
	private String [] options;
	
	public Setting(String id, String name, String group, String label, String [] options) {
		this.id = id;
		this.name = name;
		this.group = group;
		this.label = label;
		this.options = options;
	}
	
	public String getId() {
		return this.id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getGroup() {
		return this.group;
	}
	
	public String getLabel() {
		return this.label;
	}
	
	public String [] getOptions() {
		return this.options;
	}

	@Override
	public int compareTo(Setting another) {
		return this.id.compareTo(another.id);
	}	
}
