# Resource Encryption API
API's to facillitate the encryption of Sling resource values

# When to use this
Due to policies, you may encounter use cases where stored data should be encrypted so that read access to the JCR does not mean that you have access to the encrypted data.

Examples of policies:
1. Requirement to secure sensitive data at rest
2. Prevent operational and/or support from being able to view sensitive data stored in the JCR

The Resource Encryption utils provide a framework for data to be encrypted programatically or handled automatically as part of a data submission via a POST.

# How to use this
* Configure a KeyProvider
* Configure an EncryptionProvider to target the Configured KeyProvider

Once the EncryptionProvider is configured, the EncryptPostProcessor and the EncryptableValueMapAdatperFactory will be enabled 

# Overview of the API's
## EncryptableValueMap
Wraps the encryption and decryption process of resource values so that a common framework can be put in place to handle encryption needs.

## EncryptionProvider
Manages the encryption and decryption process specifically so that an entity can provide an encryption algorithm that fits the need of their organization.

## KeyProvider
Manages the retreival of Secret Keys so that they keys used to encrypt and decrypt data can be done in a robust way and with the ability to handle key rotations if needed.


# Performing Encrytpion

## Encrypting Data via POST
This bundle supplies a SingPostProcessor service that looks for for form fields to submit. This is a configurable service that can support two different method of identification

### Method 1 - @Encrypt secondary field
In the same manner of controlling content as the servlets.post bundle you are able to add an additional hidden field to a form that will let the Post servlet encrypt the value submitted in the non hidden field.

Example:
```html
    <input type="text" name="personalid"/>
    <input type="hidden" name="personalid@Encrypt" value="ignored" />
```

In the example the @Encrypt suffix in the field name lets the processor know that the value identified by the String to the left of the ampersand needs to be encrypted. 

### Method 2 - @Encrypt inline
To make it as easy as possible to identify fields that need to be encrypted this Service can be configured in the OSGi to accept fields where the suffix can be applied directly to the field name that needs to be encrypted.
```html
    <input type="text" name="personalid@Encrypt" />
```

### Configurable Suffix identifier
To avoid naming conflict or limitations applied by third party libraries. The suffix that is used to trigger the encryption is configurable as well via the OSGi console.

## Encrypting Data via code
To encrypt a property in a resource. The resource needs to be adapated to an EncryptionValueMap. Then use the encrypt method to secure the property leveraging the ModifiableValueMap class
```java
EncryptionValueMap map = resource.adaptTo(EncryptionValueMap.class);
map.encrypt("ssn");

resourceResolver.commit()
```
An exception will be thrown if attempting to modify a resource that you don't have permission to or if the resource was provided by a ResourceProvider that is not Modifiable.

When encrypt is called on a value that is already encrypted, if the encryption provider supports the decoding of the existing value, that value will be re-encrypted.


## Accessing Encrypted Values
Once a resource has an encrypted value. Access to decrypt that value is built into the normal ValueMap accessors
 ```java
EncryptionValueMap map = resource.adaptTo(EncryptionValueMap.class);
String plaintext = map.get("ssn");
```

# Encryption Providers

## AesGcmEncryptionProvider

Encrypts a String and returns the result in a Base64 format 

A standard encrypted String form this implementation takes the format of

    🔒2u7G5DT0uPXQ1606a+NERe9EVtuH6PR31MO7FjN4NZw+KKc=

This String consists of a configurable prefix to identify that the String is encrypted followed by a byte[] that has been Base64 encoded.

The decoded byte array consists of the following three segments
* IV - represents the initailization vector
* Key ID - is a unique identifier for the KeyProvider to identify the associated secret
* Encrypted data - with the remainder representing the encrypted data

Additionally the GCM encryption process takes the property name as additional authentication data. So that an encrypted value is only decryptable when it is associated with the original property name. 

# Key Providers
KeyProviders are used to provision keys to the EncryptionProvider to allow for the keys to be managed independently

## JCESKeyProvider
Utilizing the java keystore mechanism, the JCESKeyProvider requires a Java KeyStore to be created and maintained separatly from the OSGi environment. The JCESKeyProvider is used to identify the alias that is the primary key for encryption, as well as supporting secondary aliases to be used for decryption purposes.

The use of secondary aliases is to support **key rotation**. Since the encrypted string self identified the id of the alias to use, when there is a need to change the key. A new primary alias is identified and the old primary is moved to the secondary aliases.This allows for processes to continue to decrypt Strings that are stored with the old alias while simultaneously encrytping data with the new alias.
To facilitate a proper secure rotation there would still be a need to manually process the old keys and reencrypt them with the new alias. 

Re-encryption can be handled by the EncrytableValueMap as the encrypt() method will re-encrypt a value that has a supported decryption alias id.

## OSGiKeyProvider
Stores a list of Base64 encoded keys that are entered via the OSGi admin console. This is useful for testing of the Encryption Process as well as for environments where access to the underlying filesystem is limited or restricted, however serious consideration needs to be considered before relying on this method for long term storage of sensitive information.

