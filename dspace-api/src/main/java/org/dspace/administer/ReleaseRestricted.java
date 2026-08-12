/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.administer;

import org.dspace.app.util.Restrict;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;

/**
 * This class checks to see if there are any items that are due to be
 * reinstated into the archive and performs the actions necessary to do so.
 *
 * @author  Richard Jones
 */
public class ReleaseRestricted {
    
    public static void main(String [] argv)
        throws Exception
    {

        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

        String usage = "Usage: " + ReleaseRestricted.class.getName() +
            " administrator-email-address";
        
        Context c = null;
        
        try 
        {
            c = new Context();

            //EPerson ep = EPerson.findByEmail(c, argv[0]);
            EPerson ep = ePersonService.findByEmail(c, argv[0]);        

    
            if (ep != null)
            {
                c.setCurrentUser(ep);
            }
            else
            {
                System.err.println("Invalid Email Address, please enter the" +
                                    "address of the system administrator");
            }
        
            // super user
            //c.setIgnoreAuthorization(true);
        
            Item[] items = Restrict.getAvailable(c);
            System.out.println("Number of items to be de-restricted: " 
                            + items.length);
            
            Restrict.release(c, items);
            

            c.commit();
            c.complete();
        }
        catch (ArrayIndexOutOfBoundsException ae)
        {
            System.err.println(usage);

            if (c != null)
            {
                c.abort();
            }

            System.exit(1);
        }
    }
    
}