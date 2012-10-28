package net.digitalfeed.pdroidalternative;

class Setting implements Comparable<Setting> {
	private String id;
	private String name;
	private String title;
	private String group;
	private String groupTitle;
	private String [] options;
	
	public Setting(String id, String name, String title, String group, String groupTitle, String [] options) {
		this.id = id;
		this.name = name;
		this.title = title;
		this.group = group;
		this.groupTitle = groupTitle;
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
	
	public String getGroupTitle() {
		return this.groupTitle;
	}

	public String getTitle() {
		return this.title;
	}
	
	public String [] getOptions() {
		return this.options;
	}

	@Override
	public int compareTo(Setting another) {
		return this.id.compareTo(another.id);
	}	
}
