## Exercise 1
### Part A: Analysis of the Initial Prompt

Prompt: "Quero criar uma aplicação de gestão de projetos e tarefas chamada Matriz. Ajuda-me a desenvolver a aplicação."

**1. What features did the AI suggest?**

The AI suggested project management, task management, task assignment, progress tracking, and team collaboration. It also mentioned possible user roles such as project manager, team member, and administrator.

**2. What assumptions or decisions were made without confirmation?**

The AI assumed that the application would need user roles, task assignments, deadlines, notifications, and collaboration features. It also suggested a possible MVP before the requirements had been clarified. 

**3. Did the AI propose an architecture or technologies?**

No. The response did not specify technologies or propose a concrete technical architecture.

**4. What information should have been clarified first?**

The AI should have clarified who would use the application, how projects and tasks should be managed, what information tasks should contain, how progress should be tracked, what permissions users need, and whether there are any security, privacy, or business constraints.

**5. Conclusion**

The initial prompt was too generic. Although the AI provided useful suggestions and questions, it also introduced assumptions that could influence the project scope. A more structured prompt should instruct the AI to act as a requirements analyst, identify missing information, distinguish facts from assumptions, and ask clarifying questions before proposing solutions.


### Part B: Structured Prompt

#### 1. Goal
Act as a software requirements analyst. Your goal is to analyse the problem description for Matriz and identify the information needed to understand the user's requirements before any solution is proposed.

#### 2. Context
I want to develop an application called Matriz for managing projects and tasks. The initial problem description may be incomplete or ambiguous. The requirements must be clarified before moving on to solution design or implementation.

#### 3. Constraints
- Do not propose technologies, frameworks, programming languages, or technical architectures.
- Do not implement or generate code.
- Do not treat assumptions or guesses as established facts.
- Clearly distinguish known facts from assumptions.
- Identify missing information, ambiguities, and contradictions.
- Use clear, objective language.

#### 4. Task
Analyse the problem description provided and:
1. Summarise the problem and the needs identified.
2. Separate known facts from assumptions.
3. Identify missing information, ambiguities, and potential contradictions.
4. Formulate relevant clarification questions about the application's objectives, users, workflows, and business rules.
5. Prioritise the questions according to their importance for defining the requirements.

#### 5. Validation
Before presenting your response, verify that:
- Every stated fact is supported by the problem description.
- Assumptions are explicitly labelled and are not presented as facts.
- Relevant information gaps and ambiguities have been identified.
- Clarification questions are clear, specific, and useful.
- No technologies have been proposed and no code has been generated.

Present the results in the following sections:
1. Problem summary
2. Known facts
3. Assumptions
4. Missing information and ambiguities
5. Prioritised clarification questions
6. Validation checklist

### TESTING THE PROMPT

**1. What features did the AI suggest?**

The AI identified possible features such as project creation and management, task management, task status tracking, task assignment, collaboration, permissions, notifications, and progress reporting. However, these were presented as possibilities to be clarified, rather than confirmed requirements.

**2. What assumptions or decisions were made without confirmation?**

The AI identified several assumptions, including the possibility of multiple user roles, task assignments, deadlines, task statuses, collaboration features, and progress overviews. Importantly, it explicitly labelled these as assumptions rather than treating them as established facts. It did not make definitive decisions about the application's features or scope.

**3. Did the AI propose an architecture or technologies?**

No. The AI followed the prompt's constraints by avoiding technology recommendations, technical architecture, and code implementation.

**4. What information should have been clarified first?**

The AI identified the need to clarify the problem Matriz should solve, its target users, how projects and tasks should be organised, how responsibilities and permissions should work, how task progress should be tracked, which collaboration features are necessary, and what should be included in the initial version. It prioritised these questions according to their importance for defining the requirements.

**5. Conclusion**

The structured prompt produced a more systematic and requirements-focused response than the general prompt. By explicitly defining the AI's role, context, constraints, tasks, and validation criteria, it encouraged the AI to distinguish known facts from assumptions, identify missing information, and formulate prioritised clarification questions. It also prevented premature technical decisions and code generation. This demonstrates that structured prompting improves clarity, control, and alignment with the intended objective, although the quality of the analysis still depends on the information provided.

### Advantages of structured prompt
An unstructured prompt is vague and open to interpretation. The response may be too broad and the AI will make many assumptions.
A structured prompt reduces ambiguity about the expected result, separates confirmed information from assumptions and provides a checklist to verify compliance.
The AI seeks relevant information and asks clarifying questions - it doesnt produce fewer ideas but changes how those ideas are presented.
