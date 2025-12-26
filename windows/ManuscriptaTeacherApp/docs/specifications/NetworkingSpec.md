# Networking Specifications (Windows)

## Explanatory Note

This document defines the Windows application's networking specifications, which describe the bidirectional communication between its Electron frontend and its ASP.NET Core backend.


### Section 1 — General Principles

(1) The backend of the application, built with ASP.NET Core, must define a SignalR Hub class. This Hub must:

    (a) be responsible for managing client-server connections, providing server methods, and invoking client methods.

    (b) be exposed via ASP.NET Core at the URL endpoint `/hub`.

(2) The frontend of the application, built with Electron (HTML/CSS/JS/React), must use a JavaScript-based SignalR client for ASP.NET Core. This client must be responsible for invoking hub methods and handling server messages.



### Section 2 - Establishing a Connection and Bidirectional Communication

(1) When the backend is run,

    (a) it must expose its hub at the endpoint `/hub`.


(2) When the frontend is run,

    (a) it must start and initialise a SignalR client.

    (b) it must connect to the SignalR Hub endpoint `/hub`, exposed by the backend.


(3) After a connection is established,
`
    (a) The frontend may send messages to the backend by invoking server methods via its SignalR client.

    (b) The backend may send messages to the frontend through its hub.



### Section 3 - Backend methods

(1) The backend SignalR Hub must provide the frontend with exposed server methods for all functionalities described below.

    (a) CRUD methods for:
        
        (i) Unit collections.
        
        (ii) Units.
        
        (iii) Source materials belonging to a given unit.

        (iv) Lessons.

        (v) Materials.

    (b) Method for retrieving all student statuses.

    (c) Methods for 

        (i) Locking or unlocking any number of tablets.

        (ii) Deploying or ending a live session.

        (iii) Giving feedback to a response.

    (d) Methods for sending AI assistant prompts.

    (e) Methods for updating app settings.




### Section 4 - Frontend handlers

(1) The frontend JavaScript client must include handlers to perform all functionalities described below.

    (a) Handlers for

        (i) When at least one device status has changed.

        (ii) When a response to a material has been received.

        (iii) When a AI assistant response has been generated.