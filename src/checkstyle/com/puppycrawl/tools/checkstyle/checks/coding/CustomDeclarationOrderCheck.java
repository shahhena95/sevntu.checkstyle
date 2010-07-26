////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2010  Oliver Burn
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////
package com.puppycrawl.tools.checkstyle.checks.coding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.beanutils.ConversionException;

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.api.Utils;

public class CustomDeclarationOrderCheck extends Check
{
    /** List of order declaration customizing by user */
    private final ArrayList<FormatMatcher> mCustomOrderDeclaration = new ArrayList<FormatMatcher>();

	@Override
	public int[] getDefaultTokens()
	{
		final HashSet<String> classMembers = new HashSet<String>(); //HashSet for unique Tokens

		for (FormatMatcher currentRule : mCustomOrderDeclaration) {
			 classMembers.add(currentRule.mClassMember); //add Tokens
		}

		int defaultTokens[] = new int [classMembers.size()];
		int i = 0;

		for (String token : classMembers) {  //instead of iterator
			if (token.equals("Field")) {
				defaultTokens[i] = TokenTypes.VARIABLE_DEF;
			}
			else if (token.equals("CTOR")) {
				defaultTokens[i] = TokenTypes.CTOR_DEF;
			}
			else if (token.equals("Method")){
				defaultTokens[i] = TokenTypes.METHOD_DEF;
			}
			else if (token.equals("InnerClass")){
				defaultTokens[i] = TokenTypes.CLASS_DEF;
			}
			i++;
		}
		return defaultTokens;
	}

	@Override
	public void visitToken(DetailAST aAST)
	{
		//final int parentType = aAST.getParent().getType();
		aAST = aAST.findFirstToken(TokenTypes.MODIFIERS);
		String str = getUniteModifiersList(aAST);
		switch (aAST.getType()){
		case TokenTypes.CLASS_DEF:

		}
	}

	@Override
	public void leaveToken(DetailAST aAST)
	{
        if (aAST.getType() == TokenTypes.OBJBLOCK) {
           // mScopeStates.pop();
        }
	}

	private String getUniteModifiersList(DetailAST aAST)
	{
		String modifiers = "";
		for (int i = 0; i < aAST.getChildCount(); i++) {
				//aAST = aAST.findFirstToken(TokenTypes.MODIFIERS).getFirstChild();
				if (aAST.findFirstToken(TokenTypes.ANNOTATION) != null)
					aAST = aAST.findFirstToken(TokenTypes.ANNOTATION);
				modifiers += concatLogic(aAST.getFirstChild());
				aAST = aAST.getNextSibling();
		}
		return modifiers;
	}
	
	private String concatLogic(DetailAST aAST){
		String modifiers = "";
		while (aAST != null) {
			if (aAST.getType() == TokenTypes.ANNOTATION
					|| aAST.getType() == TokenTypes.EXPR) {
				modifiers += concatLogic(aAST.getFirstChild());
				aAST = aAST.getNextSibling();
			}
			modifiers += aAST.getText();
			aAST = aAST.getNextSibling();		
		}
		return modifiers;
	}

	/**
	 * Parsing input line with custom declaration order into massive
	 * @param aInputOrderDeclaration The string line with the user custom declaration 
	 */
	public void setCustomDeclarationOrder(String aInputOrderDeclaration) {
		for (String currentState : aInputOrderDeclaration.split("\\s*###\\s*")) {
			mCustomOrderDeclaration.add(new FormatMatcher(currentState));
		}
	}

    /**
     * Set whether or not the match is case sensitive.
     * @param aCaseInsensitive true if the match is case insensitive.
     */
	public void setIgnoreRegExCase(boolean aCaseInsensitive)
	{
		if (aCaseInsensitive) {
			if (mCustomOrderDeclaration.size() != 0) {
				for (FormatMatcher currentRule : mCustomOrderDeclaration) {
					currentRule.setCompileFlags(Pattern.CASE_INSENSITIVE);
				}
			}
			else {
				FormatMatcher.setFlags(Pattern.CASE_INSENSITIVE);
			}
		}
	}
    
    /**
     * private class for members of class and their patterns
     */
	private static class FormatMatcher
    {
	 /** mClassMember position in parsed input massive*/
	 private static final int mClassMemberPosition = 0;
	 /** mRegExp position in parsed input massive*/
	 private static final int mRegExpPosition = 1; 
	 /** 
	  * The flags to create the regular expression with. <br>
	  * Default compile flag is 0 (the default).
	  */
	 private static int mCompileFlags = 0;
     /** The regexp to match against */
     private Pattern mRegExp;
     /** The Member of Class*/
     private String mFormat;
     /** The string format of the RegExp */
     private String mClassMember;
     
     /**
      * Creates a new <code>FormatMatcher</code> instance. 
      * Parse into Member and RegEx.
      * Defaults the compile flag to 0 (the default).
      * @param aInputRule input string with ClassDefinition and RegEx
      * @throws ConversionException unable to parse aDefaultFormat
      */
     public FormatMatcher(String aInputRule)
         throws ConversionException
     {
         this(aInputRule, mCompileFlags);     
     }

     /**
      * Creates a new <code>FormatMatcher</code> instance.
      * @param aInputRule input string with ClassDefinition and RegEx
      * @param aCompileFlags the Pattern flags to compile the regexp with.
      * See {@link Pattern#compile(java.lang.String, int)}
      * @throws ConversionException unable to parse aDefaultFormat
      */
		public FormatMatcher(final String aInputRule, final int aCompileFlags)
				throws ConversionException, ArrayIndexOutOfBoundsException
		{
			String inputRule[] = aInputRule.split("[()]");
			String aDefaultFormat = null;
			mClassMember = inputRule[mClassMemberPosition].trim();

			if (inputRule.length < 2)
				// if RegExp is empty
				aDefaultFormat = "$^"; // the empty RegExp
			else
				aDefaultFormat = inputRule[mRegExpPosition];

			updateRegexp(aDefaultFormat, aCompileFlags);
		}

 	/**
 	 * Saving compile flags for further usage 
 	 * @param aCompileFlags the aCompileFlags to set
 	 */
 	public static final void setFlags(int aCompileFlags) {
 		mCompileFlags = aCompileFlags;
 	}

     /** @return the RegExp to match against */
     public final Pattern getRegexp()
     {
         return mRegExp;
     }

     /**
      * Set the compile flags for the regular expression.
      * @param aCompileFlags the compile flags to use.
      */
     public final void setCompileFlags(int aCompileFlags)
     {
         updateRegexp(mFormat, aCompileFlags);
     }

     /**
      * Updates the regular expression using the supplied format and compiler
      * flags. Will also update the member variables.
      * @param aFormat the format of the regular expression.
      * @param aCompileFlags the compiler flags to use.
      */
     private void updateRegexp(String aFormat, int aCompileFlags)
     {
         try {
             mRegExp = Utils.getPattern(aFormat, aCompileFlags);
             mFormat = aFormat;
         }
         catch (final PatternSyntaxException e) {
             throw new ConversionException("unable to parse " + aFormat, e);
         }
     }
 }

}