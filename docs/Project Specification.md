## **Manuscripta Project Specification**

Raphael Li, Nemo Shu, Priya Bargota, Will Stephen

**Version:** 1.00

### **1\. Project Overview**

This document provides the detailed specifications for the Manuscripta Project, which aims to develop a classroom orchestration system that enables teachers to use on-device Generative AI (GenAI) to create and distribute educational materials onto students’ e-ink devices, such as those supplied by Boox or AiPaper.

The system consists of two main components: (1) a Windows application for teachers which runs on an Intel- or Qualcomm-based AI-powered PC; and (2) an Android application for students which runs on low-distraction E-ink displays. This system aims to provide the benefits of digital and AI-enhanced learning while mitigating the disruption often caused by full-colour tablets and computers in a classroom setting.

This project is supervised by Professor Dean Mohamedally, with partners including Qualcomm and the Granite+Interactions Group at IBM.

### **2\. Background and Motivation**

#### **Problem Description**

Head teachers have expressed concern that while full-colour tablet computers facilitate the use of modern AI in creating customised teaching materials, they may sometimes prove disruptive in the classroom, as students are easily distracted by other non-educational content, reducing overall learning efficiency.

Currently existing solutions to this include the installation of classroom management software on standard iPads or Android tablets (e.g. Samsung Galaxy Tab). However, the full-colour nature of these devices fails to resolve the issue of audiovisual distractions.

Another approach is to use e-ink devices with monochromatic displays, but most commercially available e-ink tablet models, like those for iPads and Android tablets, lack the computational power to effectively deliver GenAI features.

#### 

#### **Motivation**

Manuscripta tackles this issue by decoupling teachers’ GenAI content creation from students’ content consumption. The former will be carried out on the teacher’s AI-ready laptop computer, while the latter will be performed on the students’ simple e-ink devices.

This project is motivated by the desire to:

1) reduce classroom distraction by using black-and-white e-ink displays for students;  
2) empower teachers by allowing them to use on-device GenAI on their laptops to create and orchestrate lesson content like text summaries, quizzes and worksheets;  
3) boost student engagement by providing a bidirectional system where students can discreetly submit answers for quizzes and polls, which helps capture feedback from all students regardless of their potential diffidence; and  
4) support diverse cognitive loads through features like adjustable reading levels, a key requirement for partners like the National Autistic Society.

### **3\. Scope**

The project’s scope includes the development of the following:

1) a native Windows application for teachers;  
2) a corresponding native Android application specifically for e-ink tablets, of which there are assumed to be approximately 30 per class;  
3) the integration of on-device AI models, utilising OpenVINO and Qualcomm Snapdragon technologies, for teachers to generate lesson materials;  
4) the aggregation of anonymised student responses for quizzes and polls; and  
5) bidirectional communication between the teacher app and the student apps operating exclusively over a Local Area Network (LAN).

Items considered out of scope include:

1) any cloud-based functionality;  
2) the collection or management of any personally identifiable student data;  
3) running GenAI models directly on the E-ink devices;  
4) performing AI-driven image generation; and  
5) support for student devices other than Android-based E-ink tablets.

### **4\. Requirements Specification: Teacher Application**

#### This section lists the requirements associated with the teacher application. The first instance of each keyword or term is italicised.

The requirements are grouped into the following categories: lesson materials (MAT), classroom control (CON), networking (NET), accessibility (ACC) and system requirements (SYS).

**LESSON MATERIALS (MAT)**

**MAT1**	The application must provide a *Lesson Library* to store, manage and organise all generated content.

**MAT2**	The Lesson Library must be organised using the following hierarchical structure. The Lesson Library consists of zero or more *units*; each unit consists of zero or more *lessons*; and each lesson consists of zero or more *materials*.

**MAT3**	[REMOVED] See MAT3A.

**MAT3A**	The Lesson Library must be organised using the following hierarchical structure. The Lesson Library consists of zero or more *units*; each unit consists of zero or more *lessons*; and each lesson consists of zero or more *materials*.

**MAT4**	The application must include a search function for teachers to locate materials across all units and lessons.

**MAT5**	The application must allow a teacher to generate and store materials using one or more on-device GenAI models.

**MAT6**	When creating a new material, the application must prompt the teacher for the material type.

**MAT7**	When creating a new material, the application must prompt the teacher for a textual description of the expected content.

**MAT8**	When creating a new material, the application must prompt the teacher to upload zero or more source material documents. Supported file formats must include .pdf, .txt and .docx.

**MAT9**	[REMOVED] See MAT17.

**MAT10**	[REMOVED] See MAT17.

**MAT11**	[REMOVED] See MAT17.

**MAT12**  After creating a new material, the teacher must be able to modify and refine its contents via an editor.

**MAT13**	 Each material should be associated with metadata such as its deployment status and creation date.

**MAT14** The application could provide an AI-powered conversational teaching assistance to offer teachers support with lesson planning and differentiation strategies through a chat interface.

**MAT15** The application must allow the import and display of static images and PDF documents as lesson materials.

**MAT16** The application must allow the teacher to highlight and define keywords or vocabulary for each material.

**MAT17**	 When creating a new material, the system must provide a means, such as through a slider, for adjusting the text complexity and readability of the generated material, by selecting a target age group (e.g. “the readability should match a typical 8-year-old’s reading level”) as well as a target reading age level, such as that suggested by a Progressive Skills test.

**MAT18**	 When deploying a material, the application should provide the option to mark individual student’s responses either manually or with AI assistance.

**MAT19** When deploying a material, the application should provide an optional point system where points are awarded for correct responses as positive reinforcement.

**CLASSROOM CONTROL (CON)**

**CON1**	The application must provide a continuously updated *dashboard* displaying aggregated and anonymised data.

**CON2**   [REMOVED] See CON2A.

**CON2A**   The dashboard must include an overview, in the form of a colour-coded grid, of the individual statuses of each student tablet. Each cell of the grid must clearly state whether the student is on task, needs help, is disconnected, is locked, or is idle.

**CON3**	The dashboard must include a panel displaying the number of connected devices.

**CON4**	The dashboard could include a panel displaying the average student’s progress through the current lesson. This is calculated based on all connected devices and displayed as a percentage.

**CON5**	The dashboard must include a panel indicating devices which require attention. This includes students who are in need of assistance or whose tablets are low on battery.

**CON6**	The application must include a panel providing device-specific controls, including the ability to lock selected screens or to end the session for selected students.

**CON7**	The dashboard must include a chart summarising students’ responses to quizzes or polls, if applicable.

**CON8**	The dashboard must include a panel for launching and delivering a previously created material to all students.

**CON9**	[REMOVED] See CON14.

**CON10** The dashboard must support simultaneous differentiation, allowing the teacher to deploy different materials (e.g., high, middle, low ability worksheets) to specific sub-groups of devices at the same time.

**CON11** The application must allow the teacher to export and save session data (such as student progress and quiz results) to a local file for offline tracking.

**CON12** The dashboard must provide a visual alert when a specific student triggers a "Help" request from their device.

**CON13** The dashboard could provide a "Live View" allowing the teacher to view the current screen of a specific student's device to assist with troubleshooting. This will be supported via status update messages that include student view data.

**CON14** The dashboard information should be split into two different tabs or sections. This allows the teacher to share their screen without simultaneously disclosing sensitive information.

**NETWORKING (NET)**

**NET1**	The application must be able to distribute material content to at least 30 students’ tablets through the same LAN.

**NET2**	The application must be able to receive responses, such as those to polls and quizzes, from student tablets.

**ACCESSIBILITY (ACC)**

**ACC1**	The user interface must be intuitive for teachers with standard computer literacy.

**ACC2**	The teacher must be able to configure their application settings and preferences on their laptop.

**ACC3**	[REMOVED] See ACC3A.

**ACC3A**	The teacher must be able to enable and disable accessibility features, such as text-to-speech buttons, AI summary options and animated avatars, on a per tablet basis.

**SYSTEM REQUIREMENTS (SYS)**

**SYS1**	The application must be compatible with both Intel and Qualcomm AI chipsets.

**SYS2**	The application could be secured through local user authentication.

**SYS3**	The application must be packaged for distribution via the Microsoft Store.

### **5\. Requirements Specification: Student Application**

#### This section lists the requirements associated with the student application.

The requirements are grouped into the following categories: lesson materials (MAT), accessibility (ACC) and system requirements (SYS).

**LESSON MATERIALS (MAT)**

**MAT1**	The application must be able to display all supported material types, such as quizzes, worksheets and polls.

**MAT2**	[REMOVED] See MAT2A.

**MAT2A**	When answering questions, the application must provide immediate and formative feedback to submitted responses, displaying a "Correct" (✓) or "Not quite right" (✗) message.

**MAT3**	When answering questions, the application must provide a "Try Again" option for incorrect responses.

**MAT4**	The application must include buttons to either simplify, expand on or summarise the text in the currently displayed material.

**MAT5**	The application must provide students with AI assistance tools, possibly through a chat interface, to dynamically scaffold and guide their learning.

**MAT6** The application must provide a dedicated area or highlighting mechanism to display the "Key Vocabulary" defined by the teacher for the current lesson.

**MAT7** The application must include a "Raise Hand" button that sends a help request to the teacher's dashboard.

**MAT8** The application must support handwriting input, allowing students to annotate directly onto worksheets or PDFs using a stylus.

**MAT9** Question feedback should identify the correct parts of their reasoning and guide them towards the right answer, without directly giving it to them. This assumes the use of dynamic feedback generation as opposed to static pre-defined responses.

**ACCESSIBILITY (ACC)**

**ACC1**	The application must support tapping, typing and the use of styluses as input methods.

**ACC2**	The application could support eye gaze control via third-party hardware.

**ACC3**	If enabled by the teacher, the application must include a text-to-speech button which, when pressed, reads the on-screen text aloud.

**ACC4**	The application must have a monochromatic display with minimal audiovisual stimuli to avoid distractions.

**ACC5  \-** If enabled by the teacher, the application should include animated mascots or avatars to act as learning companions.

### **NETWORKING (NET)**

**NET1**	The application must be able to receive material content from the teacher’s laptop through a LAN.

**NET2**	The application must be able to send responses, such as those to polls and quizzes, to the teacher’s laptop.

**SYSTEM REQUIREMENTS (SYS)**

**SYS1**	The application must be compatible with commercially available e-ink tablets.

### **6\. System-wide requirements**

The following requirements apply to the system as a whole, including both teacher and student applications.

**1**	The system must robustly perform data validation on all user inputs.

**2**	The system must display clear messages for empty states (e.g., "no results found") and loading states (e.g., throbbers during AI generation).

**3**	All student performance data recorded by the system must be anonymised and must not be traceable to an individual user.

**4**	The system must support simultaneous connections and interactions from up to 30 student devices on a standard LAN without significant latency.

### **7\. Constraints and assumptions**

The above requirements are set with respect to the following constraints.

1. The teacher's laptop and all student devices must be connected to the same LAN.  
2. The teacher's device must be a Windows PC with an AI-capable chipset and sufficient computational power to run the required GenAI models locally.  
3. The student devices must be Android-based e-ink tablets.  
4. Student data privacy is of paramount importance. All collected data must be anonymised.

In addition, it is assumed that the school's LAN infrastructure is capable of handling concurrent traffic from over 30 devices.

### **8\. System Design and Architecture**

This section describes the architecture and framework to be used for each application.

**Teacher Application (Windows):**

**Platform:** Windows

**Language/Framework:** .NET

**AI Model(s):** Granite

**Considerations:**

* The product of the framework must be easily put on the Microsoft Store  
* The Programming language must be supported by the chosen AI model (e.g. IBM Granite), or can be easily integrated  
* The Programming Language & Framework should be able to support CRUD and database integration  
  **AI Libraries:** OpenVINO, Qualcomm AI Stack (e.g., "executorch")

**Student Application (E-ink):**

**Platform:** Android

**Language/Framework:** Java, Room (Database), Retrofit (Networking) and Hilt (dependency injection).

**Considerations:**

* Android clients should be lightweight and low latency.  
* All content loaded onto the android client must be greyscale.  
* The app must support kiosk mode. 

**Networking Protocol:**

**Client-server relationship:** the Android Tablets should act as the client and the Windows Laptop should act as the server.

Communication should mainly be done by the HTTP protocol, since it is anticipated that most content would be transmitted as text. However, lower-level message transmission using TCP/UDP on certain control messages should be considered if a performance bottleneck is discovered.

Data Consistency and Entity Identification:

Entity IDs (for materials, lessons, units, questions, responses, sessions, etc.) must be persistent and consistent across both the Windows teacher application and Android student applications. When either client generates a new entity, it must assign a globally unique identifier (e.g., UUID) that remains constant across all services. This ensures data integrity and enables proper synchronization without conflicts. For example, if the Windows application creates a new material, it assigns the ID; if an Android client creates a response, it assigns that response's ID. All services must respect and preserve these IDs throughout the entity lifecycle.

### **9\. Schedule and Key Milestones**

The following table outlines the key deadlines and deliverables for the project.

| Date | Milestone or Deliverable |
| :---- | :---- |
| 22 Oct 2025 | **Checkpoint 1:** Client outreach, meeting schedules established, team roles appointed, and initial specification draft begun. |
| 5 Nov 2025 | **HCI Assignment Submission:** 10-slide PowerPoint file detailing prototype development, user needs analysis, and evaluation. |
| 12 Nov 2025 | **Specification Draft:** Draft project specification and Gantt chart completed. |
| 20 Jan 2026 | **Group Presentation:** Remote presentation of project progress and/or initial findings. |
| 27 Mar 2026 | **Final Submission:** In-person submission of the final group report and the system artefact. |

### **10\. Ethical, Legal and Sustainability Considerations**

The Manuscripta Project is subject to the following ethical, legal and sustainability considerations.

* **Data Privacy:** This is the highest consideration. The system must not store personally identifiable student data. All performance tracking must be aggregated and anonymised, as confirmed by project partners. The system must be compliant with relevant data protection laws (e.g., GDPR).  
* **Accessibility:** A key consideration is accessibility. The project’s core requirement is the use of E-ink displays to reduce classroom distraction. We acknowledge that this hardware choice, as noted by project partner Dan Cooper, does not support the advanced accessibility features (like eye-gaze) available on full-colour tablets. This is accepted as a known limitation of the project's current scope, which prioritises the "low-distraction" requirement from the head teachers.  
* **Sustainability:** By leveraging existing devices (teacher laptops) for heavy computation and using low-power E-ink displays for students, the system may have a lower overall energy footprint compared to a 1:1 laptop or full-colour tablet solution.

