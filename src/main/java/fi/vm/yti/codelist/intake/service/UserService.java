package fi.vm.yti.codelist.intake.service;

import java.util.UUID;

import fi.vm.yti.codelist.intake.dto.UserDTO;

public interface UserService {

    void updateUsers();

    UserDTO getUserById(final UUID id);
}
