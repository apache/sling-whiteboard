# Troubleshooting Guide

## Overview
This guide helps you diagnose and resolve common issues with Apache Sling MCP Server.

## Common Issues

### MCP Server Not Responding
- Verify the MCP servlet is registered and active
- Check OSGi component status
- Review log files for errors

### Bundle Issues
- Check bundle state (ACTIVE, RESOLVED, INSTALLED)
- Verify all dependencies are satisfied
- Use the OSGi diagnostic tools

### Performance Problems
- Review recent requests and response times
- Check system resources
- Analyze thread dumps if available

### Component Registration Issues
- Verify component configurations
- Check service dependencies
- Review component lifecycle logs

## Diagnostic Tools
The MCP Server provides several diagnostic tools:
- Bundle state inspection
- Component resource analysis
- Recent request tracking
- Log file access
- OSGi diagnostic reports

## Getting Help
If you continue to experience issues:
1. Gather diagnostic information using the MCP tools
2. Review the Apache Sling documentation
3. Check the Apache Sling mailing lists
4. File an issue in the Apache Sling JIRA
