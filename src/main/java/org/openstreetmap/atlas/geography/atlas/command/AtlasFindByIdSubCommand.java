package org.openstreetmap.atlas.geography.atlas.command;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.atlas.geography.atlas.Atlas;
import org.openstreetmap.atlas.geography.atlas.items.AtlasEntity;
import org.openstreetmap.atlas.geography.atlas.items.AtlasObject;
import org.openstreetmap.atlas.utilities.runtime.Command;
import org.openstreetmap.atlas.utilities.runtime.CommandMap;

public class AtlasFindByIdSubCommand extends AbstractAtlasSubCommand
{
    private static final Command.Switch<Set<String>> FEATURE_ID_PARAMETER = new Command.Switch<>(
            "id", "list of comma-delimited Atlas extended feature identifier",
            possibleMultipleOSMIdentifier -> Stream.of(possibleMultipleOSMIdentifier.split(","))
                    .collect(Collectors.toSet()),
            Command.Optionality.REQUIRED);

    private static final Command.Switch<String> JOINED_OUTPUT_PARAMETER = new Command.Switch<>(
            "joinedOutput", "The Atlas file to save the joined output to", String::toString,
            Command.Optionality.OPTIONAL);

    private final Set<Long> ids = new HashSet<>();

    private final Set<String> shardNames = new HashSet<>();

    public AtlasFindByIdSubCommand()
    {
        super("find-id",
                "find which atlas files contain particular Atlas features using a given set of Atlas identifiers");
    }

    @Override
    public Command.SwitchList switches()
    {
        return super.switches().with(FEATURE_ID_PARAMETER, JOINED_OUTPUT_PARAMETER);
    }

    @Override
    public void usage(final PrintStream writer)
    {
        writer.printf(AtlasCommandConstants.INPUT_PARAMETER_DESCRIPTION);
        writer.printf(
                "-id=1000000,2000000 : comma separated Atlas feature identifiers to search for\n");
    }

    @Override
    protected void start(final CommandMap command)
    {
        this.ids.addAll((Set) command.get(FEATURE_ID_PARAMETER));
    }

    @Override
    protected void handle(final Atlas atlas, final CommandMap command)
    {
        atlas.entities(idCheck()).forEach(item ->
        {
            System.out.printf(formatAtlasObject(atlas, item));
            this.shardNames.add(atlas.getName());
        });
    }

    @Override
    protected int finish(final CommandMap command)
    {
        Optional output = command.getOption(JOINED_OUTPUT_PARAMETER);
        if (output.isPresent() && !this.shardNames.isEmpty())
        {
            System.out.printf("Joining...");
            AtlasReader.main("join",
                    String.format("-input=%s", command.get(super.switches().get(0))),
                    String.format("-output=%s", output.get()),
                    String.format("-atlases=%s", String.join(",", this.shardNames)));
        }
        return 0;
    }

    private <T extends AtlasObject> Predicate<T> idCheck()
    {
        return object -> this.ids.contains(((Long) object.getIdentifier()).toString());
    }

    private String formatAtlasObject(final Atlas atlas, final AtlasEntity entity)
    {
        final String shardName = atlas.metaData().getShardName().orElse("UNKNOWN");
        return String.format("[%s] [%d] [%d] --> [%s:%s] Tags: [%s]\n", entity.getType(),
                entity.getOsmIdentifier(), entity.getIdentifier(), shardName, atlas.getName(),
                entity.getTags());
    }
}
