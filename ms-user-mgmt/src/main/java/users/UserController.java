package users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import users.User;
import users.UserRepository;

@Controller
@RequestMapping(path="/users/")
public class UserController {
	@Autowired
	private UserRepository userRepository;

	// TODO: change to PostMapping
	@GetMapping(path="/add")
	public @ResponseBody User addNewUser (@RequestParam String name) {
		User user = new User();
		user.setName(name);
		userRepository.save(user);
		return user;
	}

	@GetMapping(path="/get")
	public @ResponseBody User getUser (@RequestParam Integer id) {
		User user = userRepository.findById(id).get();
		return user;
	}

	// TODO: change to PostMapping
	@GetMapping(path="/edit")
	public @ResponseBody User editUser (@RequestParam Integer id, @RequestParam String name) {
		User user = userRepository.findById(id).get();
		user.setName(name);
		userRepository.save(user);
		return user;
	}

	@GetMapping(path="/all")
	public @ResponseBody Iterable<User> getAllUsers() {
		return userRepository.findAll();
	}
}
