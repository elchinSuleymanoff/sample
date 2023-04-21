package com.suleymanoff.elchin.sample;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;


/**
 * Handle Rest requests for customer.
 *
 * @author Elchin Suleymanov
 * @version 1.0
 * @since 2021-04-15
 */
@Controller
public class CustomerController {

    // ----------------------------Fields ----------------------------------------------
    CustomerDao customerDao;
    ProductDao productDao;

    // ----------------------------Constructors ----------------------------------------------

    /**
     *
     * @param customerDao to inject customerDao dependency
     * @param productDao to inject productDao dependency
     */
    public CustomerController(CustomerDao customerDao, ProductDao productDao) {
        this.customerDao = customerDao;
        this.productDao = productDao;
    }

    // ----------------------------Methods ----------------------------------------------


    /**
     * a get mapping for customer registration page
     *
     * @param theModel Model attribute for the customer
     * @return a String with the name of jsp page
     */
    @RequestMapping("/customerRegister")
    public String showForm(Model theModel) {
        theModel.addAttribute("customer", new Customer());
        return "customerRegister";
    }


    /**
     * a get mapping for customer login page
     *
     * @param theModel Model attribute for the customer
     * @return a String with the name of jsp page
     */
    @RequestMapping("/customerLogin")
    public String loginForm(Model theModel) {
        theModel.addAttribute("customer", new Customer());
        return "customerLogin";
    }


    /**
     * a get mapping for about us page
     *
     * @return a String with the name of jsp page
     */
    @RequestMapping("/aboutUs")
    public String aboutUsPage() {
        return "aboutUs";
    }


    /**
     * a get mapping for contact us page
     *
     * @return a String with the name of jsp page
     */
    @RequestMapping("/contactUs")
    public String contactUsPage() {
        return "contactUs";
    }



    /**
     * a get mapping for customer logout page
     *
     * @param session the session to be killed
     * @param theModel Model attribute for the customer
     * @return login page for logging in again
     */
    @RequestMapping("/customerLogout")
    public String logoutPage(HttpSession session, Model theModel) {
        session.invalidate(); //kill the session
        theModel.addAttribute("customer", new Customer());  //refresh the model attribute
        return "customerLogin";
    }


    /**
     * a get mapping for customer portal
     *
     * @param theModel Model attribute for the customer
     * @param request to get the customer session
     * @return a String with the name of jsp page
     */
    @RequestMapping("/customerPortal")
    public String customerPortal(Model theModel, HttpServletRequest request) {
        request.getSession().getAttribute("custSession"); //get the customer session
        List<Product> productList= customerDao.customerPortalViewProduct(); //display product from db
        theModel.addAttribute("plist", productList); //supply plist attribute to the view
        theModel.addAttribute("customer", new Customer()); //supply customer attribute to the view
        return "customerPortal";
    }



    /**
     * a get mapping when customer filled in register form and press register button
     *
     * @param customer bind all parameters to customer object
     * @param br hold the result of validation from customer object
     * @param theModel Model attribute for the customer (thymeleaf)
     * @return a String with the name of jsp page
     */
    @RequestMapping("/registerForm")
    public String processForm(@Valid @ModelAttribute("customer") Customer customer, BindingResult br, Model theModel) {

        theModel.addAttribute("msg", ""); //supply empty message to the view
        if (br.hasErrors()) //check validation, if validation not pass
        {
            return "customerRegister";  //return customer register page
        } else {
            //check if this customer username and email is already exist in the db ( must be unique )
            if (customerDao.checkUnique(customer.getUsername(), customer.getEmail()) == true) {
                //if exist
                theModel.addAttribute("msg", "Username or Email has already exist."); //supply error message to the view
                return "customerRegister"; //to this view
            } else {
                //if not ex
                theModel.addAttribute("msg", "Account has been registered successfully, please log in."); //supply error message to the view
                theModel.addAttribute("customer", customer); //and add this customer as model attribute
                customerDao.register(customer); //save the customer info into db
                return "customerLogin"; //return customer login page
            }

        }
    }



    /**
     * a post mapping when customer wants to login
     *
     * @param customer bind all parameters to customer object
     * @param theModel Model attribute for the customer
     * @param request to set the customer session
     * @return either log in to the portal or send back to login page
     */
    @RequestMapping(value = "/loginForm", method = RequestMethod.POST)
    public String processLoginForm(@ModelAttribute("customer") Customer customer, Model theModel, HttpServletRequest request) {
        //check if this customer and password is exist in the database
        if (customerDao.login(customer.getUsername(), customer.getPassword()) == true) {
            //if exist
            request.getSession().setAttribute("custSession", customer.getUsername()); //set username as the session attribute
            List<Product> plist = customerDao.customerPortalViewProduct(); //get product from db to show customer
            String username = (String) request.getSession().getAttribute("custSession"); //get the session attribute
            List<Item> temp_cart = productDao.customerViewTempCart(username);  //get the temporary cart for this specific user
            int totalQty = 0;
            for (int i = 0; i < temp_cart.size(); i++) {
                totalQty = totalQty + temp_cart.get(i).getQuantity(); //count the total quantity in cart
            }
            request.getSession().setAttribute("totalQty", totalQty);//set the total quantity as session attribute
            theModel.addAttribute("temp_cart", temp_cart);  //supply tempory cart to the view
            theModel.addAttribute("plist", plist);  //supply product list to the view
            theModel.addAttribute("customer", customer); //supply this customer to the view
            return "customerPortal"; //return to the customer portal page
        } else {
            //not exist
            theModel.addAttribute("msg", "Invalid Credential. Please try again."); //supply error message
            return "customerLogin";//to customer login page
        }

    }



    /**
     * a get mapping for customer when they want to change their address
     * @param username to retrieve username from url
     * @param theModel to view customer details
     * @return a String to updateAddress jsp page
     */
    @RequestMapping(value = "/updateAddress/{username}")    //path variable to get the username
    public String updateAddress(@PathVariable String username, Model theModel) {
        Customer c = customerDao.getCustomerByUsername(username); //get customer by username
        String address = c.getAddress(); //get customer address
        String email = c.getEmail(); //get customer email
        theModel.addAttribute("email", email);//display customer email
        theModel.addAttribute("address", address); //display customer address
        theModel.addAttribute("customer", new Customer());
        return "updateAddress";  //return update address page
    }



    /**
     * a get mapping for customer when they enter the new address and click on submit button
     *
     * @param username to retrieve username from url
     * @param address request the new address to update the existing one
     * @param theModel to view customer attributes
     * @param request to set the customer session
     * @return
     */
    @RequestMapping(value = "/updateAddressForm", method = RequestMethod.POST)
    public String updateOrderStatusForm(@RequestParam("username") String username, @RequestParam("address") String address, Model theModel, HttpServletRequest request) {
        //request param to get the value from form
        Customer c = new Customer(); //call beans
        c.setUsername(username); //set username
        c.setAddress(address); //set address
        customerDao.updateAddress(c); //update address where username = this.username
        Customer cu = customerDao.getCustomerByUsername(username); //get customer by username
        String add = cu.getAddress(); //get new address
        String email = cu.getEmail(); //get customer email
        theModel.addAttribute("email", email); //display email
        theModel.addAttribute("address", add); //display new address
        theModel.addAttribute("customer", new Customer());
        theModel.addAttribute("msg", "Your address has been updated successfully."); //display success message.
        return "updateAddress";//return update address page
    }

}