package io.github.shirohoo.realworld.application.profile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.shirohoo.realworld.domain.user.Profile;
import io.github.shirohoo.realworld.domain.user.User;
import io.github.shirohoo.realworld.domain.user.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTests {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    String jakeToken;

    @BeforeEach
    void setUp() throws Exception {
        User jake = User.builder()
                .email("jake@jake.jake")
                .password(passwordEncoder.encode("jakejake"))
                .profile(Profile.builder()
                        .username("jake")
                        .bio("I work at statefarm")
                        .build())
                .build();

        User james = User.builder()
                .email("james@james.james")
                .password(passwordEncoder.encode("jamesjames"))
                .profile(Profile.builder()
                        .username("james")
                        .bio("I work at statefarm")
                        .build())
                .build();

        userRepository.save(jake);
        userRepository.save(james);

        MockHttpServletResponse response = mockMvc.perform(
                        post("/api/users/login")
                                .header("Content-Type", "application/json")
                                .content(
                                        """
                        {
                          "user":{
                            "email": "jake@jake.jake",
                            "password": "jakejake"
                          }
                        }
                        """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.user.email").value("jake@jake.jake"))
                .andExpect(jsonPath("$.user.token").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("jake"))
                .andExpect(jsonPath("$.user.bio").value("I work at statefarm"))
                .andExpect(jsonPath("$.user.image").isEmpty())
                .andReturn()
                .getResponse();

        String jsonContent = response.getContentAsString();
        jakeToken = JsonPath.read(jsonContent, "$.user.token");
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Succeed in following")
    void shouldSucceedFollow() throws Exception {
        mockMvc.perform(post("/api/profiles/james/follow").header("Authorization", "Bearer " + jakeToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("james"))
                .andExpect(jsonPath("$.profile.bio").value("I work at statefarm"))
                .andExpect(jsonPath("$.profile.image").isEmpty())
                .andExpect(jsonPath("$.profile.following").value(true));
    }

    @Test
    @DisplayName("Response 404 when following, if the followee does not exist in the DB")
    void shouldFailFollowIfFolloweeDoesNotExistInDB() throws Exception {
        mockMvc.perform(post("/api/profiles/noUser/follow").header("Authorization", "Bearer " + jakeToken))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Succeed in unfollowing")
    void shouldSucceedUnfollow() throws Exception {
        // follow
        mockMvc.perform(post("/api/profiles/james/follow").header("Authorization", "Bearer " + jakeToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("james"))
                .andExpect(jsonPath("$.profile.bio").value("I work at statefarm"))
                .andExpect(jsonPath("$.profile.image").isEmpty())
                .andExpect(jsonPath("$.profile.following").value(true));

        // unfollow
        mockMvc.perform(delete("/api/profiles/james/follow").header("Authorization", "Bearer " + jakeToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("james"))
                .andExpect(jsonPath("$.profile.bio").value("I work at statefarm"))
                .andExpect(jsonPath("$.profile.image").isEmpty())
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    @DisplayName("Response 404 when unfollowing, if the followee is not in the DB")
    void shouldFailUnfollowIfFolloweeDoesNotExistInDB() throws Exception {
        mockMvc.perform(delete("/api/profiles/noUser/follow").header("Authorization", "Bearer " + jakeToken))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Expect `following` to be `true` when get profile within logged in")
    void shouldSucceedGetProfileWithinLogin() throws Exception {
        // follow
        mockMvc.perform(post("/api/profiles/james/follow").header("Authorization", "Bearer " + jakeToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("james"))
                .andExpect(jsonPath("$.profile.bio").value("I work at statefarm"))
                .andExpect(jsonPath("$.profile.image").isEmpty())
                .andExpect(jsonPath("$.profile.following").value(true));

        // expect `following` to be `true` when get profile
        mockMvc.perform(get("/api/profiles/james").header("Authorization", "Bearer " + jakeToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("james"))
                .andExpect(jsonPath("$.profile.bio").value("I work at statefarm"))
                .andExpect(jsonPath("$.profile.image").isEmpty())
                .andExpect(jsonPath("$.profile.following").value(true));
    }

    @Test
    @DisplayName("Expect `following` to be `false` when get profile without logged in")
    void shouldSucceedGetProfileWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/profiles/james").header("Authorization", "Bearer " + jakeToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("james"))
                .andExpect(jsonPath("$.profile.bio").value("I work at statefarm"))
                .andExpect(jsonPath("$.profile.image").isEmpty())
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    @DisplayName("Response 404 if when fetch profiles, if the target does not exist in the DB")
    void shouldFailGetProfileIfUserDoesNotExistInDB() throws Exception {
        mockMvc.perform(delete("/api/profiles/noUser/follow").header("Authorization", "Bearer " + jakeToken))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}
