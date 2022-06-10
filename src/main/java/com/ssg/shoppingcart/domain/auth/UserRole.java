package com.ssg.shoppingcart.domain.auth;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.ssg.shoppingcart.domain.BaseTimeStampEntity;
import com.ssg.shoppingcart.domain.User;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ssg_user_role")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class UserRole extends BaseTimeStampEntity {

  @Id
  @Column(name = "ssg_user_role_id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(targetEntity = User.class, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JoinColumn(name = "ssg_user_id")
  @JsonBackReference
  private User user;

  @ManyToOne(targetEntity = Role.class, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JoinColumn(name = "ssg_role_id")
  @JsonBackReference
  private Role role;

  @Builder
  public UserRole(Long id, User user, Role role) {
    this.id = id;
    this.user = user;
    this.role = role;
  }
}