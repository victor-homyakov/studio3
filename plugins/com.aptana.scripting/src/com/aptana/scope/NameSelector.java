package com.aptana.scope;

public class NameSelector implements ISelectorNode
{
	private String _name;
	
	/**
	 * NameSelector
	 * 
	 * @param name
	 */
	public NameSelector(String name)
	{
		this._name = name;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.aptana.scope.ISelectorNode#matches(com.aptana.scope.MatchContext)
	 */
	public boolean matches(MatchContext context)
	{
		boolean result = false;
		
		if (context != null && this._name != null && this._name.length() > 0)
		{
			String step = context.getCurrentStep();
			
			if (step != null && step.startsWith(this._name))
			{
				// step matches as a prefix, now make sure we matched the whole step
				// or up to a period
				int nameLength = this._name.length();
				int scopeLength = step.length();
				
				if (result = (scopeLength == nameLength || step.charAt(nameLength) == '.'))
				{
					context.advance();
				}
			}
		}
		
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return this._name;
	}
}
