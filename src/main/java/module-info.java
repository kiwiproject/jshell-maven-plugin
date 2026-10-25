module org.kiwiproject.jshell {

   requires jdk.jshell;
   /**not modules yet*/
   requires maven.plugin.api;
   requires maven.plugin.annotations;
   requires maven.artifact;

   exports org.kiwiproject.jshell;

   uses javax.tools.Tool;
}
