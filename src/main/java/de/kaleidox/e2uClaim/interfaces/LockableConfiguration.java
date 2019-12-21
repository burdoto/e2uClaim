package de.kaleidox.e2uClaim.interfaces;

public interface LockableConfiguration extends Deserializable {
    boolean checkPassword(String password);
}
