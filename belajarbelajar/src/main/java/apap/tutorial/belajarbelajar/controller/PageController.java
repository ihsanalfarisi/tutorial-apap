package apap.tutorial.belajarbelajar.controller;

import apap.tutorial.belajarbelajar.model.UserModel;
import apap.tutorial.belajarbelajar.security.xml.Attributes;
import apap.tutorial.belajarbelajar.security.xml.ServiceResponse;
import apap.tutorial.belajarbelajar.service.RoleService;
import apap.tutorial.belajarbelajar.service.UserService;
import apap.tutorial.belajarbelajar.setting.Setting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.List;

@Controller
public class PageController {

    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;

    @Autowired
    ServerProperties serverProperties;

    private WebClient webClient = WebClient.builder().build();

    @RequestMapping("/")
    public String home(Model model){
        List<UserModel> listUser = userService.getListUser();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        UserModel userModel = userService.getUserByUsername(username);

        model.addAttribute("listUser", listUser);
        model.addAttribute("user", userModel);
        return "home";
    }

    @RequestMapping("/login")
    public String login(Model model) {
        model.addAttribute("port", serverProperties.getPort());
        return "login";
    }

    @GetMapping("/validate-ticket")
    public ModelAndView adminLoginSSO(
            @RequestParam(value = "ticket", required = false) String ticket,
            HttpServletRequest request
    ){
        ServiceResponse serviceResponse = this.webClient.get().uri(
                String.format(
                        Setting.SERVER_VALIDATE_TICKET,
                        ticket,
                        Setting.CLIENT_LOGIN
                )
        ).retrieve().bodyToMono(ServiceResponse.class).block();

        Attributes attributes = serviceResponse.getAuthenticationSuccess().getAttributes();
        String username = serviceResponse.getAuthenticationSuccess().getUser();

        UserModel user = userService.getUserByUsername(username);

        if(user == null){
            user = new UserModel();
            user.setEmail(username + "@ui.ac.id");
            user.setNama(attributes.getNama());
            user.setPassword("belajarbelajar");
            user.setUsername(username);
            user.setIsSso(true);
            user.setRole(roleService.getById(Long.valueOf(1)));
            userService.addUser(user);
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(username, "belajarbelajar");
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);

        HttpSession httpSession = request.getSession(true);
        httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        return new ModelAndView("redirect:/");
    }

    @GetMapping(value = "/login-sso")
    public ModelAndView loginSSO(){
        return new ModelAndView("redirect:" + Setting.SERVER_LOGIN + Setting.CLIENT_LOGIN);
    }

    @GetMapping(value = "/logout-sso")
    public ModelAndView logoutSSO(Principal principal){
        UserModel user = userService.getUserByUsername(principal.getName());
        if (user.getIsSso()==false){
            return new ModelAndView("redirect:/logout");

        }
        return new ModelAndView("redirect:" + Setting.SERVER_LOGOUT + Setting.CLIENT_LOGOUT);
    }
}

