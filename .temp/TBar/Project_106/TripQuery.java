package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import org.opentripplanner.ext.transmodelapi.TransmodelGraphQLPlanner;
import org.opentripplanner.ext.transmodelapi.model.DefaultRoutingRequestType;
import org.opentripplanner.ext.transmodelapi.model.EnumTypes;
import org.opentripplanner.ext.transmodelapi.model.TransportModeSlack;
import org.opentripplanner.ext.transmodelapi.model.framework.LocationInputType;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.routing.api.request.RequestFunctions;

public class TripQuery {


  public static GraphQLFieldDefinition create(
      DefaultRoutingRequestType routing,
      GraphQLOutputType tripType,
      GqlUtil gqlUtil
  ) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("trip")
        .description(
            "Input type for executing a travel search for a trip between two locations. Returns "
            + "trip patterns describing suggested alternatives for the trip."
        )
        .type(tripType)
        .argument(GraphQLArgument.newArgument()
            .name("dateTime")
            .description(
                "Date and time for the earliest time the user is willing to start the journey "
                + "(if arriveBy=false/not set) or the latest acceptable time of arriving "
                + "(arriveBy=true). Defaults to now"
            )
            .type(gqlUtil.dateTimeScalar)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("searchWindow")
            .description(
                "The length of the search-window in minutes. This is normally dynamically "
                + "calculated by the server, but you may override this by setting it. The "
                + "search-window used in a request is returned in the response metadata. To get "
                + "the \"next page\" of trips use the metadata(searchWindowUsed and "
                + "nextWindowDateTime) to create a new request. If not provided the value is "
                + "resolved depending on the other input parameters, available transit options and "
                + "realtime changes."
            )
            .type(Scalars.GraphQLInt)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("from")
            .description("The start location")
            .type(new GraphQLNonNull(LocationInputType.INPUT_TYPE))
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("to")
            .description("The end location")
            .type(new GraphQLNonNull(LocationInputType.INPUT_TYPE))
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("arriveBy")
            .description("Whether the trip should depart at dateTime (false, the default), or arrive at dateTime.")
            .type(Scalars.GraphQLBoolean)
            .defaultValue(routing.request.arriveBy)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("wheelchairAccessible")
            .description(
                "Whether the trip must be wheelchair accessible. Supported for the street part to "
                + "the search, not implemented for the transit jet."
            )
            .type(Scalars.GraphQLBoolean)
            .defaultValue(routing.request.wheelchairAccessible)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("ignoreRealtimeUpdates")
            .description("When true, realtime updates are ignored during this search.")
            .type(Scalars.GraphQLBoolean)
            .defaultValue(routing.request.ignoreRealtimeUpdates)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("locale")
            .description(
                "The preferable language to use for text targeted the end user. Note! The data "
                    + "quality is limited, only stop and quay names are translates, and not in all "
                    + "places of the API."
            )
            .type(EnumTypes.LOCALE)
            .defaultValue("no")
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("modes")
            .description("The set of access/egress/direct/transit modes to be used for this search.")
            .type(ModeInputType.INPUT_TYPE)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("banned")
            .description("Banned")
            .description("Parameters for indicating authorities, lines or quays not be used in the trip patterns")
            .type(BannedInputType.INPUT_TYPE)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("whiteListed")
            .description("Whitelisted")
            .description("Parameters for indicating the only authorities, lines or quays to be used in the trip patterns")
            .type(JourneyWhiteListed.INPUT_TYPE)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("walkSpeed")
            .description("The maximum walk speed along streets, in meters per second.")
            .type(Scalars.GraphQLFloat)
            .defaultValue(routing.request.walkSpeed)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("walkReluctance")
            .description("Walk cost is multiplied by this value. This is the main parameter to use for limiting walking.")
            .type(Scalars.GraphQLFloat)
            .defaultValue(routing.request.walkReluctance)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("waitReluctance")
            .description(
                "Wait cost is multiplied by this value. Setting this to a value lower than 1 "
                + "indicates that waiting is better than staying on a vehicle. This should never "
                + "be set higher than walkReluctance, since that would lead to walking down a line "
                + "to avoid waiting."
            )
            .type(Scalars.GraphQLFloat)
            .defaultValue(routing.request.waitReluctance)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("bikeSpeed")
            .description("The maximum bike speed along streets, in meters per second")
            .type(Scalars.GraphQLFloat)
            .defaultValue(routing.request.bikeSpeed)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("bicycleOptimisationMethod")
            .description(
                "The set of characteristics that the user wants to optimise for during bicycle "
                + "searches -- defaults to "
                + enumValAsString(EnumTypes.BICYCLE_OPTIMISATION_METHOD, routing.request.optimize)
            )
            .type(EnumTypes.BICYCLE_OPTIMISATION_METHOD)
            .defaultValue(routing.request.optimize)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("transferPenalty")
            .description(
                "An extra penalty added on transfers (i.e. all boardings except the first one). "
                + "The transferPenalty is used when a user requests even less transfers. In the "
                + "latter case, we don't actually optimise for fewest transfers, as this can lead "
                + "to absurd results. Consider a trip in New York from Grand Army Plaza (the one "
                + "in Brooklyn) to Kalustyan's at noon. The true lowest transfers trip pattern is "
                + "to wait until midnight, when the 4 train runs local the whole way. The actual "
                + "fastest trip pattern is the 2/3 to the 4/5 at Nevins to the 6 at Union Square, "
                + "which takes half an hour. Even someone optimise for fewest transfers doesn't "
                + "want to wait until midnight. Maybe they would be willing to walk to 7th Ave "
                + "and take the Q to Union Square, then transfer to the 6. If this takes less than "
                + "transferPenalty seconds, then that's what we'll return."
            )
            .type(Scalars.GraphQLInt)
            .defaultValue(routing.request.transferCost)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("transferSlack")
            .description(
                "An expected transfer time (in seconds) that specifies the amount of time that "
                + "must pass between exiting one public transport vehicle and boarding another. "
                + "This time is in addition to time it might take to walk between stops."
            )
            .type(Scalars.GraphQLInt)
            .defaultValue(routing.request.transferSlack)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("boardSlackDefault")
            .description(TransportModeSlack.boardSlackDescription("boardSlackList"))
            .type(Scalars.GraphQLInt)
            .defaultValue(routing.request.boardSlack)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("boardSlackList")
            .description(TransportModeSlack.slackByGroupDescription(
                "boardSlack", routing.request.boardSlackForMode
            ))
            .type(TransportModeSlack.SLACK_LIST_INPUT_TYPE)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("alightSlackDefault")
            .description(TransportModeSlack.alightSlackDescription("alightSlackList"))
            .type(Scalars.GraphQLInt)
            .defaultValue(routing.request.alightSlack)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("alightSlackList")
            .description(TransportModeSlack.slackByGroupDescription(
                "alightSlack", routing.request.alightSlackForMode
            ))
            .type(TransportModeSlack.SLACK_LIST_INPUT_TYPE)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("numTripPatterns")
            .description(
                "The maximum number of trip patterns to return. Note! This reduce the number of "
                + "trip patterns AFTER the OTP travel search is done in a post-filtering process. "
                + "There is little performance gain in reducing the number of trip patterns "
                + "returned. The post-filtering will reduce the number of trip-patterns down to "
                + "this size. It does not make the search faster, as it did in OTP1. See also the "
                + "trip meta-data on how to implement paging."
            )
            .defaultValue(routing.request.numItineraries)
            .type(Scalars.GraphQLInt)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("maximumTransfers")
            .description("Maximum number of transfers. Note! The best way to reduce the number of "
                + "transfers is to set the `transferPenalty` parameter.")
            .type(Scalars.GraphQLInt)
            .defaultValue(routing.request.maxTransfers)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("transitGeneralizedCostLimit")
            .description("Set a relative limit for all transit itineraries. The limit "
                + "is calculated based on the best transit itinerary generalized-cost. "
                + "Itineraries without transit legs are excluded from this filter. "
                + "Example: f(x) = 3600 + 2.0 x. If the lowest cost returned is 10 000, "
                + "then the limit is set to: 3 600 + 2 * 10 000 = 26 600. Then all "
                + "itineraries with at least one transit leg and a cost above 26 600 "
                + "is removed from the result. Default: "
                + RequestFunctions.serialize(routing.request.transitGeneralizedCostLimit)
            )
            .type(gqlUtil.doubleFunctionScalar)
            // There is a bug in the GraphQL lib. The default value is shown as a `boolean`
            // with value `false`, not the actual value. Hence; The default is added to the
            // description above instead. .defaultValue(routing.request.transitGeneralizedCostLimit)
            .build()
        )
        .argument(GraphQLArgument.newArgument()
            .name("debugItineraryFilter")
            .description(
                "Debug the itinerary-filter-chain. The filters will mark itineraries as deleted, "
                + "but NOT delete them when this is enabled."
            )
            .type(Scalars.GraphQLBoolean)
            .defaultValue(routing.request.debugItineraryFilter)
            .build()
        )
        .dataFetcher(environment -> new TransmodelGraphQLPlanner().plan(environment))
        .build();
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  private static String enumValAsString(GraphQLEnumType enumType, Enum<?> otpVal) {
    return enumType
        .getValues()
        .stream()
        .filter(e -> e.getValue().equals(otpVal))
        .findFirst()
        .get()
        .getName();
  }

}
