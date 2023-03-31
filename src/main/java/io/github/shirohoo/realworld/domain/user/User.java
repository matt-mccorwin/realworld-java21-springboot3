package io.github.shirohoo.realworld.domain.user;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.annotation.JsonRootName;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@JsonRootName("user")
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Builder.Default
    @Column(unique = true)
    private UUID guid = UUID.randomUUID();

    private String password;

    @Column(unique = true)
    private String email;

    @Embedded
    private Profile profile;

    @Transient
    private String token;

    @ManyToMany
    @JoinTable(
            name = "follow",
            joinColumns = @JoinColumn(name = "following_id"),
            inverseJoinColumns = @JoinColumn(name = "follower_id"))
    private Set<User> followers = new HashSet<>();

    @ManyToMany(mappedBy = "followers")
    private Set<User> following = new HashSet<>();

    public static User from(UserVO user) {
        return User.builder()
                .email(user.email())
                .password(user.password())
                .profile(Profile.builder()
                        .username(user.username())
                        .bio(user.bio())
                        .image(user.image())
                        .build())
                .token(user.token())
                .build();
    }

    public UserVO toImmutable() {
        String username = this.profile.getUsername();
        String bio = this.profile.getBio();
        String image = this.profile.getImage();
        return new UserVO(username, this.email, "masked", bio, image, this.token);
    }

    public Profile getProfile(User me) {
        if (me.following.contains(this)) return new Profile(this.profile, true);
        else return this.profile;
    }

    public Profile follow(User target) {
        this.following.add(target);
        target.followers.add(this);
        return target.getProfile(this);
    }

    public Profile unfollow(User target) {
        this.following.remove(target);
        target.followers.remove(this);
        return target.getProfile(this);
    }

    public User encryptPasswords(PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(this.password);
        return this;
    }

    public User bind(String token) {
        this.token = token;
        return this;
    }

    public boolean sameUsername(String username) {
        return this.profile.getUsername().equals(username);
    }

    public boolean sameEmail(String email) {
        return this.email.equals(email);
    }

    public User updateUser(PasswordEncoder passwordEncoder, UserVO updateRequests) {
        if (updateRequests.username() != null) {
            this.profile.setUsername(updateRequests.username());
        }

        if (updateRequests.password() != null) {
            this.password = passwordEncoder.encode(updateRequests.password());
        }

        if (updateRequests.email() != null) {
            this.email = updateRequests.email();
        }

        this.profile.setBio(updateRequests.bio());

        this.profile.setImage(updateRequests.image());

        return this;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.profile.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
