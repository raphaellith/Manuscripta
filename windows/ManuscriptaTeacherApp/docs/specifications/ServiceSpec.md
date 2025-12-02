# Service Layer Specifications (Windows)

## Explanatory Note: Purpose of the Service layer

The Service layer provides the interface which downstream layers should use to access the models, and acts as a safeguard which enforces data integrity.

## Section 1 - General Principles

(1) No service may access Data Entities directly without using polymorphic Entity classes.

(2) The Service layer should be functionally complete in relation to any downstream layer. That is to say, the service layer should remove the need of any downstream layer to access any upstream layers directly.

(3) The Service layer, collectively with all upstream layers, must enforce all constraints defined in the Data Validation Specification. This requirement will not be reiterated in later Sections.

(4) In this specification —

    (a) The service layer contains any class placed in the `/Main/Services` directory.
    (b) An 'upstream layer' means a layer which the Service layer will make calls to. This includes the Entity layer.
    (c) A 'downstream layer' means a layer which will make calls to the Service layer. This includes the Controller layer.

## Section 2 - Services and required functionalities

(1) The Service layer should include Material Service class(es) which manages materials, and their related questions. The class(es) must provide the following high-level functionalities:

    (a) Creating a material of any valid type.
    (b) Creating a question of any valid type.
    (c) Modifying existing data fields on a material or a question.
    (d) Retrieving all materials.
    (e) Retrieving all questions under a material.
    (f) Retrieving a material by its uuid.
    (g) Retrieving a question by its uuid.
    (h) Deleting a question.
    (i) Deleting a material, and all associated questions.

(2) The Service layer should include a Response Service class which manages responses to questions. The class must provide the following high-level functionalities:

    (a) Creating a response to a question.
    (b) Modifying existing data fields on a response.
    (c) Retrieving all responses under a question.
    (d) Retrieving a response by its uuid.
    (e) Deleting a response.