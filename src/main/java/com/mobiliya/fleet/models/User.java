package com.mobiliya.fleet.models;

@SuppressWarnings({"ALL", "unused"})
public class User {

    private String id;
    private int status;
    private String tenantId;
    private String roleId, fleetId;
    private String firstName, lastName, email, mobileNumber, password, tenantCompany;

    public String getTenantCompany() {
        return tenantCompany;
    }

    public void setTenantCompany(String tenantCompany) {
        this.tenantCompany = tenantCompany;
    }

    public User() {
    }

    public String getFleetId() {
        return fleetId;
    }

    public void setFleetId(String fleetId) {
        this.fleetId = fleetId;
    }

    public User(String id, String email, String firstName, String lastName, String mobile, int status, String roleId, String fleetId, String tenantid) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.mobileNumber = mobile;
        this.status = status;
        this.roleId = roleId;
        this.fleetId = fleetId;
        //this.password = password;
        this.tenantId = tenantid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        email = email;
    }


    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

}
