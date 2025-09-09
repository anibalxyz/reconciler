package com.anibalxyz.users.infra;

import com.anibalxyz.persistence.EntityManagerProvider;
import com.anibalxyz.users.domain.Email;
import com.anibalxyz.users.domain.User;
import com.anibalxyz.users.domain.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import java.util.List;
import java.util.Optional;

public class JpaUserRepository implements UserRepository {

  private final EntityManagerProvider provider;

  public JpaUserRepository(EntityManagerProvider provider) {
    this.provider = provider;
  }

  private EntityManager em() {
    return provider.get();
  }

  @Override
  public Optional<User> findById(Integer id) {
    UserEntity userEntity = em().find(UserEntity.class, id);
    return userEntity == null ? Optional.empty() : Optional.of(userEntity.toDomain());
  }

  @Override
  public Optional<User> findByEmail(Email email) {
    try {
      UserEntity userEntity =
          em().createQuery("SELECT u FROM UserEntity u WHERE u.email = :email", UserEntity.class)
              .setParameter("email", email.value())
              .getSingleResult();
      return Optional.of(userEntity.toDomain());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  @Override
  public List<User> findAll() {
    List<UserEntity> userEntityList =
        em().createQuery("SELECT u FROM UserEntity u", UserEntity.class).getResultList();
    return userEntityList.stream().map(UserEntity::toDomain).toList();
  }

  @Override
  public User save(User user) {
    return em().merge(UserEntity.fromDomain(user)).toDomain();
  }

  @Override
  public boolean deleteById(Integer id) {
    UserEntity userEntity = em().find(UserEntity.class, id);
    if (userEntity != null) {
      em().remove(userEntity);
      return true;
    }
    return false;
  }
}
