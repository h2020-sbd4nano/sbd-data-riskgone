// Copyright (c) 2022-2023  Egon Willighagen <egon.willighagen@gmail.com>
//
// GPL v3

@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.3.3')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.3.3')
@Grab(group='io.github.egonw.bacting', module='net.bioclipse.managers.jsoup', version='0.3.3')

bioclipse = new net.bioclipse.managers.BioclipseManager(".");
rdf = new net.bioclipse.managers.RDFManager(".");
jsoup = new net.bioclipse.managers.JSoupManager(".");

kg = rdf.createInMemoryStore()

// the next should be extracted from the sitemap.xml. This is just 6 of 34
pages = [
  "https://ammar257ammar.github.io/Nanosafety-data-reusability-34-datasets/overview/9e1d426c90.html",
  "https://ammar257ammar.github.io/Nanosafety-data-reusability-34-datasets/overview/9a9ad3fe96.html",
  "https://ammar257ammar.github.io/Nanosafety-data-reusability-34-datasets/overview/1687649488.html",
  "https://ammar257ammar.github.io/Nanosafety-data-reusability-34-datasets/overview/5d2e680699.html",
  "https://ammar257ammar.github.io/Nanosafety-data-reusability-34-datasets/overview/0a77bd4afd.html",
  "https://ammar257ammar.github.io/Nanosafety-data-reusability-34-datasets/overview/51cbf2d4ae.html"
]

for (page in pages) {
  htmlContent = bioclipse.download(page)
  htmlDom = jsoup.parseString(htmlContent)

  // application/ld+json
  bioschemasSections = jsoup.select(htmlDom, "script[type='application/ld+json']");

  for (section in bioschemasSections) {
    bioschemasJSON = section.html()
    rdf.importFromString(kg, bioschemasJSON, "JSON-LD")
  }
}

turtle = rdf.asTurtle(kg);

println "#" + rdf.size(kg) + " triples detected in the JSON-LD"
println turtle

sparql = """
PREFIX schema: <https://schema.org/>
SELECT ?dataset ?url ?name ?license ?description WHERE {
  ?dataset a schema:Dataset ;
    schema:url ?url .
  OPTIONAL { ?dataset schema:name ?name }
  OPTIONAL { ?dataset schema:license ?license }
  OPTIONAL { ?dataset schema:description ?description }
} ORDER BY ASC(?dataset)
"""

results = rdf.sparql(kg, sparql)

println "@prefix dc:    <http://purl.org/dc/elements/1.1/> ."
println "@prefix dct:   <http://purl.org/dc/terms/> ."
println "@prefix foaf:  <http://xmlns.com/foaf/0.1/> ."
println "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> ."
println "@prefix sbd:   <https://www.sbd4nano.eu/rdf/#> ."
println "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> ."
println "@prefix void:  <http://rdfs.org/ns/void#> ."
println ""
println "<https://ammar257ammar.github.io/Nanosafety-data-reusability-34-datasets/>"
println " a                    void:DatasetDescription ;"
println " dc:source            <https://ammar257ammar.github.io/Nanosafety-data-reusability-34-datasets/> ;"
println " dct:title            \"Data completeness and reusability assessment for 34 nanosafety datasets\"@en ;"
println " dct:license          <https://github.com/ammar257ammar/Nanosafety-data-reusability-34-datasets/blob/main/LICENSE> ."
println ""

for (i=1;i<=results.rowCount;i++) {
  println "<${results.get(i, "dataset")}> a sbd:Dataset ;"
  println "  dc:source <https://ammar257ammar.github.io/Nanosafety-data-reusability-34-datasets/> ;"
  if (results.get(i, "name") != null) println "  rdfs:label \"${results.get(i, "name")}\"@en ;"
  if (results.get(i, "description") != null) println "  dc:description \"${results.get(i, "description")}\"@en ;"
  if (results.get(i, "license") != null) println "  dct:license <${results.get(i, "license")}> ;"
  println "  foaf:page <${results.get(i, "url")}> ."
  println ""
}
