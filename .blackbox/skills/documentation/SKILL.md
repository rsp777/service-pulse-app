---
name: documentation
description: Generates, formats, and maintains high-quality technical documentation for codebases, APIs, and projects. Use this skill when instructed to write READMEs, API specifications, docstrings, or user guides.
---

# Documentation

## Instructions
When utilizing this skill, follow these step-by-step guidelines to ensure clear and comprehensive documentation:

1. **Analyze the Request:** Determine the scope of the documentation needed (e.g., inline comments, architectural overview, API reference, or end-user guide).
2. **Identify the Audience:** Adjust the technical depth and tone based on whether the reader is a developer, stakeholder, or end-user.
3. **Structure the Content:** Use standard Markdown formatting. Include clear headings, bulleted lists for readability, and language-specific code blocks.
4. **Detail Inputs and Outputs:** For code-level documentation (like functions or API endpoints), explicitly list parameters, return types, expected formats, and potential error states.
5. **Keep It Concise but Thorough:** Avoid unnecessary jargon. Focus on the *why* and *how* rather than just repeating the code line-by-line.
6. **Review for Consistency:** Ensure naming conventions, formatting, and explanations remain consistent throughout the document.

## Examples

### Example 1: Documenting a Function
**Context:** The user provides a snippet of a data processing function and asks for documentation.
**Execution:** The agent analyzes the function and generates a standard docstring (e.g., Javadoc, PEP 257) that describes the function's purpose, time complexity, arguments, return values, and includes a brief usage example.

### Example 2: Generating a Project README
**Context:** The user asks, "Create a README for my new repository."
**Execution:** The agent creates a structured Markdown file containing:
- Project Title & Description
- Prerequisites
- Installation Instructions
- Usage Guide with examples
- Contributing Guidelines
- License Information

### Example 3: API Endpoint Specification
**Context:** The user provides a backend route block and says, "Document this endpoint."
**Execution:** The agent generates an API specification detailing:
- **URL path:** `/api/v1/resource`
- **Method:** `POST`
- **Headers Needed:** `Authorization: Bearer <token>`
- **Request Body:** Required JSON fields and data types.
- **Success Response:** Status code `201 Created` and sample JSON output.
- **Error Responses:** Expected status codes (e.g., `400 Bad Request`, `401 Unauthorized`) and the scenarios that trigger them.