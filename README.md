nark
====

Nark is an alerting and dashboard frontend for graphite.

Targets
In Nark, one can use all the syntax that Graphite understands. In addition to this, Nark understands additional syntax that makes viewing dashboards more convenient. The following section describes how to use this functionality in a declaring targets.

  Variables
    Nark targets can include variables. These are represented as text inputs in the dashboard view An example of how this can be helpful is when a dashboard should allow viewers to select a particular server to view.
    
    A variable should be placed between two % signs. The variable name can be optionally followed by a default value, specified using an = sign.
    
      ex.   stats.counters.servers.%variable-name=default-value%.requests.count
      
    When creating additional targets and graphs, using the same variable name will result in the targets sharing the same value for that variable across the dashboard. The default value of the variable will be the first default value declared by a target. Thus, it is only necessary to provide a default value the first time a target declares the variable.
    
  Options
    Nark targets can also include options. An option provides a variable with a set of acceptable values. Options are represented in the dashboard view as a dropdown menu.
    
    An option should be declared between two % signs. The variable name is followed by | sign and value pairs. 
    
      ex.   stats.%type|counters|gauges|timers%.servers.10-0-33-33.requests.count
    
    When creating additional targets and graphs, using the same option name will result in targets sharing the same value across the dashboard. The values in the dropdown menu are the union of all values declared for that option in all targets. If a target can simply use the values of options declared elsewhere, simple put a | sign after the variable name.
    
      ex.   stats.%type|%.servers.10-0-33-33.requests.count (will inherit the values for 'type' declared in other targets)
