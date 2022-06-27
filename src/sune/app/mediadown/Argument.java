package sune.app.mediadown;

/** @since 00.02.02 */
public final class Argument {
	
	private final String name;
	private final String value;
	
	public Argument(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	public String name()  { return name; }
	public String value() { return value; }
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Argument other = (Argument) obj;
		if(name == null) {
			if(other.name != null)
				return false;
		} else if(!name.equals(other.name))
			return false;
		if(value == null) {
			if(other.value != null)
				return false;
		} else if(!value.equals(other.value))
			return false;
		return true;
	}
}