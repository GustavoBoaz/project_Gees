package com.gees.App.services;

import java.nio.charset.Charset;
import java.util.Optional;

import javax.validation.Valid;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

import com.gees.App.exceptions.models.user.ExistingEmailException;
import com.gees.App.exceptions.models.user.ValidLoginException;
import com.gees.App.exceptions.models.user.ErrorInLoginException;
import com.gees.App.exceptions.models.user.ErrorInRegistrationException;
import com.gees.App.exceptions.models.user.ErrorInTokenException;
import com.gees.App.models.UserModel;
import com.gees.App.models.dtos.UserCredentialsDTO;
import com.gees.App.models.dtos.UserLoginDTO;
import com.gees.App.models.dtos.UserRegisterDTO;
import com.gees.App.repositories.UserRepository;


@Service
public class UserServices {
	
	private @Autowired UserRepository repository;
	private UserCredentialsDTO credentials;
	private UserModel user;
	
	/**
	 * Private static method, used to encrypt with BCryptPasswordEncoder format a
	 * string passed as a parameter.
	 * 
	 * @param password, String format.
	 * @return String
	 * @author Boaz
	 * @since 1.0
	 * @see BCryptPasswordEncoder
	 * 
	 */
	private static String encryptPassword(String password) {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		return encoder.encode(password);
	}
	
	/**
	 * Private static method, used to generate token in Base64 format.
	 * 
	 * @param email,    String format.
	 * @param password, String format.
	 * @return String
	 * @author Boaz
	 * @since 1.0
	 * @see Base64
	 * 
	 */
	private static String generatorToken(String email, String password) {
		String structure = email + ":" + password; //gustaco@email.com:134652
		byte[] structureBase64 = Base64.encodeBase64(structure.getBytes(Charset.forName("US-ASCII"))); // Z3VzdGFjb0BlbWFpbC5jb206MTM0NjUy
		return new String(structureBase64);
	}
	
	/**
	 * Private static method, used to generate basic token.
	 * 
	 * @param email,    String format.
	 * @param password, String format.
	 * @return String
	 * @author Boaz
	 * @since 1.0
	 * @see Base64
	 * 
	 */
	private static String generatorTokenBasic(String email, String password) {
		String structure = email + ":" + password; //gustaco@email.com:134652
		byte[] structureBase64 = Base64.encodeBase64(structure.getBytes(Charset.forName("US-ASCII"))); // Z3VzdGFjb0BlbWFpbC5jb206MTM0NjUy
		return "Basic " + new String(structureBase64);
	}

	/**
	 * Public method used to register a user in the system's database. This method
	 * returns a BAD_REQUEST if the intention to register already has an email
	 * registered in the system, to avoid duplication. If you don't hear an existing
	 * email in the system, it returns CREATED status with user object no response.
	 * 
	 * @param newUser, UserRegistrationDTO object.
	 * @return ResponseEntity<UserModel>
	 * @author Boaz
	 * @since 1.0
	 * 
	 */
	public ResponseEntity<UserModel> registerUser(@Valid UserRegisterDTO newUser, Errors errors) {
		if(errors.hasErrors()){
			throw new ErrorInRegistrationException();
		}

		Optional<UserModel> optional = repository.findByEmail(newUser.getEmail());
		
		if (optional.isPresent()) {
			throw new ExistingEmailException(newUser.getEmail());
		} else {
			user = new UserModel();
			user.setName(newUser.getName());
			user.setEmail(newUser.getEmail());
			user.setToken(generatorToken(newUser.getEmail(), newUser.getPassword()));
			user.setPassword(encryptPassword(newUser.getPassword()));
			return ResponseEntity.status(201).body(repository.save(user));
		}
		
	}

	/**
	 * Public method to get user credentials for authentications.
	 * 
	 * @param userDto
	 * @return ResponseEntity<UserCredentialsDTO>
	 * @author Boaz
	 * @since 1.0
	 * 
	 */
	public ResponseEntity<UserCredentialsDTO> getCredentials(@Valid UserLoginDTO userDto, Errors errors) {
		if(errors.hasErrors()){
			throw new ValidLoginException();
		}

		return repository.findByEmail(userDto.getEmail()).map(resp -> {
			BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
			
			if (encoder.matches(userDto.getPassword(), resp.getPassword())) {
				
				credentials = new UserCredentialsDTO();
				credentials.setIdUser(resp.getIdUser());
				credentials.setEmail(resp.getEmail());
				credentials.setToken(resp.getToken());
				credentials.setTokenBasic(generatorTokenBasic(userDto.getEmail(), userDto.getPassword()));
				
				return ResponseEntity.status(200).body(credentials);
			} else {
				throw new ErrorInLoginException("password");
			}
			
		}).orElseThrow(() -> {
			throw new ErrorInLoginException("email");
		});
		
	}

	/**
	 * Public method to get user profile.
	 * 
	 * @param userDto
	 * @return ResponseEntity<UserModel>
	 * @author Boaz
	 * @since 1.0
	 * 
	 */
	public ResponseEntity<UserModel> getProfile(String token){
		return repository.findByToken(token)
			.map(resp -> ResponseEntity.ok(resp))
			.orElseThrow(() -> new ErrorInTokenException());
	}

}
