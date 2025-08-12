package com.anibalxyz.users.infra;

import com.anibalxyz.persistence.EntityManagerProvider;
import com.anibalxyz.users.domain.User;
import com.anibalxyz.users.domain.UserRepository;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class JpaUserRepository implements UserRepository {

  private final EntityManagerProvider provider;

  public JpaUserRepository(EntityManagerProvider provider) {
    this.provider = provider;
  }

  private EntityManager em() {
    return this.provider.get();
  }

  @Override
  public Optional<User> findById(Integer id) {
    UserEntity userEntity = this.em().find(UserEntity.class, id);
    return userEntity == null ? Optional.empty() : Optional.of(userEntity.toDomain());
  }

  @Override
  public List<User> findAll() {
    List<UserEntity> userEntityList =
        this.em().createQuery("SELECT u FROM UserEntity u", UserEntity.class).getResultList();
    return userEntityList.stream().map(UserEntity::toDomain).toList();
  }

  @Override
  public User save(User user) {
    return this.em().merge(UserEntity.fromDomain(user)).toDomain();
  }

  @Override
  public void deleteById(Integer id) {
    UserEntity userEntity = this.em().find(UserEntity.class, id);
    if (userEntity != null) {
      this.em().remove(userEntity);
    }
  }
}
