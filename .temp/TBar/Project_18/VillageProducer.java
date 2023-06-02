package amidst.mojangapi.world.icon.producer;

import java.util.List;

import amidst.documentation.ThreadSafe;
import amidst.mojangapi.world.Dimension;
import amidst.mojangapi.world.biome.Biome;
import amidst.mojangapi.world.coordinates.Resolution;
import amidst.mojangapi.world.icon.locationchecker.AllValidLocationChecker;
import amidst.mojangapi.world.icon.locationchecker.LocationChecker;
import amidst.mojangapi.world.icon.locationchecker.StructureBiomeLocationChecker;
import amidst.mojangapi.world.icon.locationchecker.VillageAlgorithm;
import amidst.mojangapi.world.icon.type.DefaultWorldIconTypes;
import amidst.mojangapi.world.icon.type.ImmutableWorldIconTypeProvider;
import amidst.mojangapi.world.oracle.BiomeDataOracle;

@ThreadSafe
public class VillageProducer extends RegionalStructureProducer<Void> {
	private static final Resolution RESOLUTION = Resolution.CHUNK;
	private static final int OFFSET_IN_WORLD = 4;
	private static final Dimension DIMENSION = Dimension.OVERWORLD;
	private static final boolean DISPLAY_DIMENSION = false;
	
	private static final long SALT = 10387312L;
	private static final byte SPACING = 32;
	private static final byte SEPARATION = 8;
	private static final boolean IS_TRIANGULAR = false;
	
	private static final int STRUCTURE_SIZE = 0;

	public VillageProducer(
			BiomeDataOracle biomeDataOracle,
			List<Biome> validBiomesForStructure,
			long worldSeed,
			boolean doComplexVillageCheck,
			boolean buggyStructureCoordinateMath) {
		
		super(RESOLUTION,
			  OFFSET_IN_WORLD,
			  getLocationCheckers(worldSeed, biomeDataOracle, validBiomesForStructure, doComplexVillageCheck),
			  new ImmutableWorldIconTypeProvider(DefaultWorldIconTypes.VILLAGE),
			  DIMENSION,
			  DISPLAY_DIMENSION,
			  worldSeed,
			  SALT,
			  SPACING,
			  SEPARATION,
			  IS_TRIANGULAR,
			  buggyStructureCoordinateMath
			 );
	}

	private static LocationChecker getLocationCheckers(
			long seed, BiomeDataOracle biomeDataOracle, List<Biome> validBiomesForStructure, boolean doComplexVillageCheck) {
		LocationChecker biome = new StructureBiomeLocationChecker(biomeDataOracle, STRUCTURE_SIZE, validBiomesForStructure);

		if(doComplexVillageCheck) {
			return new AllValidLocationChecker(biome, new VillageAlgorithm(biomeDataOracle, validBiomesForStructure));
		} else {
			return biome;
		}
	}
}
