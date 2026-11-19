package client;

// Research: docs/plans/CORE-ACCESS.md

// INTENTIONALLY DOES NOT COMPILE: member types of interfaces cannot be private.
public interface BadPrivateInterfaceMember {
    private class Member {}
}
