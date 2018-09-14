package org.openstreetmap.atlas.geography.atlas.command;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.openstreetmap.atlas.geography.MultiPolygon;
import org.openstreetmap.atlas.geography.atlas.pbf.AtlasLoadingOption;
import org.openstreetmap.atlas.geography.atlas.pbf.OsmPbfLoader;
import org.openstreetmap.atlas.geography.boundary.CountryBoundaryMap;
import org.openstreetmap.atlas.streaming.resource.File;
import org.openstreetmap.atlas.tags.filters.ConfiguredTaggableFilter;
import org.openstreetmap.atlas.utilities.configuration.StandardConfiguration;
import org.openstreetmap.atlas.utilities.runtime.Command;
import org.openstreetmap.atlas.utilities.runtime.CommandMap;
import org.openstreetmap.atlas.utilities.runtime.FlexibleSubCommand;

/**
 * This command converts an OSM PBF file to an Atlas file.
 *
 * @author bbreithaupt
 */
public class PBFToAtlasSubCommand implements FlexibleSubCommand
{
    private static final String NAME = "pbf-to-atlas";
    private static final String DESCRIPTION = "Converts a PBF to an Atlas file.";

    // Required parameters
    private static final Command.Switch<File> INPUT_PARAMETER = new Command.Switch<>("pbf",
            "Input PBF path", File::new, Command.Optionality.REQUIRED);
    private static final Command.Switch<File> OUTPUT_PARAMETER = new Command.Switch<>("output",
            "Output Atlas file path", File::new, Command.Optionality.REQUIRED);

    // Filter parameters
    private static final Command.Switch<File> AREA_FILTER_PARAMETER = new Command.Switch<>(
            "area-filter", "Path to a json filter for determining Areas", File::new,
            Command.Optionality.OPTIONAL);
    private static final Command.Switch<File> EDGE_FILTER_PARAMETER = new Command.Switch<>(
            "edge-filter", "Path to a json filter for determining Edges", File::new,
            Command.Optionality.OPTIONAL);
    private static final Command.Switch<File> NODE_FILTER_PARAMETER = new Command.Switch<>(
            "node-filter", "Path to a json filter for OSM nodes", File::new,
            Command.Optionality.OPTIONAL);
    private static final Command.Switch<File> RELATION_FILTER_PARAMETER = new Command.Switch<>(
            "relation-filter", "Path to a json filter for OSM relations", File::new,
            Command.Optionality.OPTIONAL);
    private static final Command.Switch<File> WAY_FILTER_PARAMETER = new Command.Switch<>(
            "way-filter", "Path to a json filter for OSM ways", File::new,
            Command.Optionality.OPTIONAL);
    private static final Command.Switch<File> WAY_SECTION_FILTER_PARAMETER = new Command.Switch<>(
            "way-section-filter", "Path to a json filter for determining where to way section",
            File::new, Command.Optionality.OPTIONAL);

    // Load Parameters
    private static final Command.Switch<Boolean> LOAD_AREAS_PARAMETER = new Command.Switch<>(
            "load-areas", "Whether to load Areas (boolean)", Boolean::new,
            Command.Optionality.OPTIONAL);
    private static final Command.Switch<Boolean> LOAD_EDGES_PARAMETER = new Command.Switch<>(
            "load-edges", "Whether to load Edges (boolean)", Boolean::new,
            Command.Optionality.OPTIONAL);
    private static final Command.Switch<Boolean> LOAD_LINES_PARAMETER = new Command.Switch<>(
            "load-lines", "Whether to load Lines (boolean)", Boolean::new,
            Command.Optionality.OPTIONAL);
    private static final Command.Switch<Boolean> LOAD_NODES_PARAMETER = new Command.Switch<>(
            "load-nodes", "Whether to load Nodes (boolean)", Boolean::new,
            Command.Optionality.OPTIONAL);
    private static final Command.Switch<Boolean> LOAD_POINTS_PARAMETER = new Command.Switch<>(
            "load-points", "Whether to load Points (boolean)", Boolean::new,
            Command.Optionality.OPTIONAL);
    private static final Command.Switch<Boolean> LOAD_RELATIONS_PARAMETER = new Command.Switch<>(
            "load-relations", "Whether to load Relations (boolean)", Boolean::new,
            Command.Optionality.OPTIONAL);
    private static final Command.Switch<Boolean> LOAD_CONNECTED_WAYS_PARAMETER = new Command.Switch<>(
            "load-connected-ways",
            "Whether to load connected ways that are outside country boundaries (boolean)",
            Boolean::new, Command.Optionality.OPTIONAL);

    // Country parameters
    private static final Command.Switch<Set<String>> COUNTRY_CODES_PARAMETER = new Command.Switch<>(
            "country-codes",
            "Countries from the country map to convert (comma separated ISO3 codes)",
            code -> Arrays.stream(code.split(",")).collect(Collectors.toSet()),
            Command.Optionality.OPTIONAL);
    private static final Command.Switch<File> COUNTRY_MAP_PARAMETER = new Command.Switch<>(
            "country-boundary-map", "Path to a WKT or shp file containing a country boundary map",
            File::new, Command.Optionality.OPTIONAL);
    private static final Command.Switch<Boolean> COUNTRY_SLICING_PARAMETER = new Command.Switch<>(
            "country-slicing", "Whether to perform country slicing (boolean)", Boolean::new,
            Command.Optionality.OPTIONAL);

    // Way Sectioning Parameter
    private static final Command.Switch<Boolean> WAY_SECTION_PARAMETER = new Command.Switch<>(
            "way-section", "Whether to perform way sectioning (boolean)", Boolean::new,
            Command.Optionality.OPTIONAL);

    @Override
    public String getDescription()
    {
        return DESCRIPTION;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public Command.SwitchList switches()
    {
        return new Command.SwitchList().with(INPUT_PARAMETER, OUTPUT_PARAMETER,
                AREA_FILTER_PARAMETER, EDGE_FILTER_PARAMETER, NODE_FILTER_PARAMETER,
                RELATION_FILTER_PARAMETER, WAY_FILTER_PARAMETER, WAY_SECTION_FILTER_PARAMETER,
                LOAD_AREAS_PARAMETER, LOAD_EDGES_PARAMETER, LOAD_LINES_PARAMETER,
                LOAD_NODES_PARAMETER, LOAD_POINTS_PARAMETER, LOAD_RELATIONS_PARAMETER,
                LOAD_CONNECTED_WAYS_PARAMETER, COUNTRY_CODES_PARAMETER, COUNTRY_MAP_PARAMETER,
                COUNTRY_SLICING_PARAMETER, WAY_SECTION_PARAMETER);
    }

    @Override
    public void usage(final PrintStream writer)
    {
        writer.println("-pbf=/path/to/pbf : pbf to convert");
        writer.println("-output=/path/to/output/atlas : Atlas file to output to");
        writer.println("-area-filter=/path/to/json/area/filter : json filter to determine Areas");
        writer.println("-edge-filter=/path/to/json/edge/filter : json filter to determine Edges");
        writer.println("-node-filter=/path/to/json/node/filter : json filter for OSM nodes");
        writer.println(
                "-relation-filter=/path/to/json/relation/filter : json filter for OSM relations");
        writer.println("-way-filter=/path/to/json/way/filter : json filter for OSM ways");
        writer.println("-load-areas=boolean : whether to load Areas; defaults to true");
        writer.println("-load-edges=boolean : whether to load Edges; defaults to true");
        writer.println("-load-lines=boolean : whether to load Lines; defaults to true");
        writer.println("-load-nodes=boolean : whether to load Nodes; defaults to true");
        writer.println("-load-points=boolean : whether to load Points; defaults to true");
        writer.println("-load-relations=boolean : whether to load Relations; defaults to true");
        writer.println(
                "-load-connected-ways=boolean : whether to load connected ways that are outside country boundaries; defaults to false");
        writer.println(
                "-country-codes=list,of,ISO3,codes : countries from the country map to convert");
        writer.println(
                "-country-boundary-map=/path/to/WKT/or/shp : a WKT or shp file containing a country boundary map");
        writer.println(
                "-country-slicing=boolean : whether to perform country slicing; defaults to true");
        writer.println(
                "-way-sectioning=boolean : whether to perform way sectioning; defaults to true");
        writer.println(
                "-way-section-filter=/path/to/json/way/section/filter : json filter to determine where to way section");
    }

    @Override
    public int execute(final CommandMap map)
    {
        new OsmPbfLoader((File) map.get(INPUT_PARAMETER), getAtlasLoadingOption(map))
                .saveAtlas((File) map.get(OUTPUT_PARAMETER));
        return 0;
    }

    /**
     * Get or create a {@link CountryBoundaryMap}.
     *
     * @param map
     *            {@link CommandMap}
     * @return {@link CountryBoundaryMap}
     */
    private CountryBoundaryMap getCountryBoundaryMap(final CommandMap map)
    {
        final Optional<File> countryMapOption = (Optional<File>) map
                .getOption(COUNTRY_MAP_PARAMETER);
        CountryBoundaryMap countryMap = CountryBoundaryMap
                .fromBoundaryMap(Collections.singletonMap("UNK", MultiPolygon.MAXIMUM));
        if (countryMapOption.isPresent())
        {
            if (FilenameUtils.isExtension(countryMapOption.get().getName(), "txt"))
            {
                countryMap = CountryBoundaryMap.fromPlainText(countryMapOption.get());
            }
            else if (FilenameUtils.isExtension(countryMapOption.get().getName(), "shp"))
            {
                countryMap = CountryBoundaryMap.fromShapeFile(countryMapOption.get().getFile());
            }
        }
        return countryMap;
    }

    /**
     * Creates an {@link AtlasLoadingOption} using configurable parameters.
     *
     * @param map
     *            {@link CommandMap}
     * @return {@link AtlasLoadingOption}
     */
    private AtlasLoadingOption getAtlasLoadingOption(final CommandMap map)
    {
        final CountryBoundaryMap countryMap = this.getCountryBoundaryMap(map);
        final AtlasLoadingOption options = AtlasLoadingOption
                .createOptionWithAllEnabled(countryMap);

        // Set filters
        map.getOption(AREA_FILTER_PARAMETER).ifPresent(
                filter -> new ConfiguredTaggableFilter(new StandardConfiguration((File) filter)));
        map.getOption(EDGE_FILTER_PARAMETER).ifPresent(
                filter -> new ConfiguredTaggableFilter(new StandardConfiguration((File) filter)));
        map.getOption(NODE_FILTER_PARAMETER).ifPresent(
                filter -> new ConfiguredTaggableFilter(new StandardConfiguration((File) filter)));
        map.getOption(RELATION_FILTER_PARAMETER).ifPresent(
                filter -> new ConfiguredTaggableFilter(new StandardConfiguration((File) filter)));
        map.getOption(WAY_FILTER_PARAMETER).ifPresent(
                filter -> new ConfiguredTaggableFilter(new StandardConfiguration((File) filter)));
        map.getOption(WAY_SECTION_FILTER_PARAMETER).ifPresent(
                filter -> new ConfiguredTaggableFilter(new StandardConfiguration((File) filter)));

        // Set loading options
        ((Optional<Boolean>) map.getOption(LOAD_AREAS_PARAMETER))
                .ifPresent(options::setLoadAtlasArea);
        ((Optional<Boolean>) map.getOption(LOAD_EDGES_PARAMETER))
                .ifPresent(options::setLoadAtlasEdge);
        ((Optional<Boolean>) map.getOption(LOAD_LINES_PARAMETER))
                .ifPresent(options::setLoadAtlasLine);
        ((Optional<Boolean>) map.getOption(LOAD_NODES_PARAMETER))
                .ifPresent(options::setLoadAtlasNode);
        ((Optional<Boolean>) map.getOption(LOAD_POINTS_PARAMETER))
                .ifPresent(options::setLoadAtlasPoint);
        ((Optional<Boolean>) map.getOption(LOAD_RELATIONS_PARAMETER))
                .ifPresent(options::setLoadAtlasRelation);
        ((Optional<Boolean>) map.getOption(LOAD_CONNECTED_WAYS_PARAMETER))
                .ifPresent(options::setLoadWaysSpanningCountryBoundaries);

        // Set country options
        ((Optional<Set>) map.getOption(COUNTRY_CODES_PARAMETER))
                .ifPresent(options::setAdditionalCountryCodes);
        ((Optional<Boolean>) map.getOption(COUNTRY_SLICING_PARAMETER))
                .ifPresent(options::setCountrySlicing);

        // Set way sectioning
        ((Optional<Boolean>) map.getOption(WAY_SECTION_PARAMETER))
                .ifPresent(options::setWaySectioning);

        return options;
    }
}