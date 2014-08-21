package org.pfaa.chemica.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;

import org.pfaa.chemica.fluid.GasMaterial;
import org.pfaa.chemica.fluid.IndustrialFluid;
import org.pfaa.chemica.model.Compound.Compounds;
import org.pfaa.chemica.model.Constants;
import org.pfaa.chemica.render.EntityDropParticleFX;
import org.pfaa.geologica.Geologica;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/* FIXME: It would be cool to use this for chemical spills, but 
 * this is not going to scale for arbitrary chemicals. 
 * We may need to extend BlockFluidClassic, but catch all calls
 * in order to query a tile entity and set the fluid-specific properties 
 * on the block instance before delegating to the superclass. 
 */
public class IndustrialFluidBlock extends BlockFluidClassic {

	private IndustrialFluid fluid;
	private static Material gas = new GasMaterial(MapColor.airColor);
	
	public IndustrialFluidBlock(IndustrialFluid fluid) {
		super(fluid, materialForIndustrialFluid(fluid));
		this.fluid = fluid;
	}

	private static Material materialForIndustrialFluid(IndustrialFluid fluid) {
		return fluid.isGaseous() ? gas : 
			fluid.getTemperature() > Constants.FLESH_IGNITION_TEMPERATURE ? Material.lava : 
				Material.water;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
		return this.fluid.getColor();
	}

	@Override
    @SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return side != 0 && side != 1 ? this.fluid.getFlowingIcon() : this.fluid.getStillIcon();
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		String prefix = fluid.isGaseous() ? "gas" : fluid.isSuperHeated() ? "molten" : "fluid";
		String postfix = fluid.isOpaque() ? "_opaque" : "";
		fluid.setStillIcon(register.registerIcon("chemica:" + prefix + "_still" + postfix));
		fluid.setFlowingIcon(register.registerIcon("chemica:" + prefix + "_flow" + postfix));
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void randomDisplayTick(World world, int x, int y, int z, Random rand) {
		super.randomDisplayTick(world, x, y, z, rand);
		this.trySeepEffect(world, x, y, z, rand);
	}

	private void trySeepEffect(World world, int x, int y, int z, Random rand) {
		if (rand.nextInt(10) == 0
				&& World.doesBlockHaveSolidTopSurface(world, x, y - 1, z)
				&& !world.getBlock(x, y - 2, z).getMaterial().blocksMovement()) {

			double px = x + rand.nextFloat();
			double py = y - 1.05D;
			double pz = z + rand.nextFloat();
	
			int color = fluid.getColor();
			float red = (color >> 16 & 255) / 255.0F;
	        float green = (color >> 8 & 255) / 255.0F;
	        float blue = (color & 255) / 255.0F;
			EntityFX fx = new EntityDropParticleFX(world, px, py, pz, red, green, blue);
			FMLClientHandler.instance().getClient().effectRenderer.addEffect(fx);
		}
	}
	
	// FIXME: for some reason, BlockFluidClassic returns null for this...
	@Override
	public Fluid getFluid() {
		return this.fluid;
	}
	
    /**
     * Ticks the block if it's been scheduled
     */
	@Override
    public void updateTick(World world, int x, int y, int z, Random rand)
    {
		this.tryToReact();
		if (this.getFluid().isGaseous()) {
			this.updateGas(world, x, y, z, rand);
		} else {
			super.updateTick(world, x, y, z, rand);
		}
    }
    
	private void tryToReact() {
		// TODO Auto-generated method stub
		
	}

	private void updateGas(World world, int x, int y, int z, Random rand)
    {
        int quantaRemaining = getQuantaValue(world, x, y, z);
        int expQuanta = -101;

        /* Algorithm:
         * Look in the opposite of the density direction
         *   If there is a block
         *      quantaRemaining = block.quantaRemaining 
         *      If any of the laterally adjacent blocks have < quanta than it
         *        quantaRemaining--
         *   Else
         *      quantaRemaining = max(laterallyAdjacentBlocks.quantaRemaining)-1
         *  
         *  First decision: move vertically, depends on density
         *  P(moveInDensityDir) = density / (atmosphericDensity + density)
         *  
         *  We use atmospheric density instead of the destination block density, because
         *  we are not able to truly displace other gases. Denser gases
         *  above are not replaced, while less dense gases are replaced, which is sort 
         *  of opposite. If we had true displacement, then we could have
         *  less dense gases rise above denser gases, but we are a long way from that.
         *  This algorithm will at least make less dense gases rise faster.
         *   
         *  Second decision: diffuse, depends on:
         *  * quanta value, i.e., concentration
         *  * sqrt(temperature)
         *  
         *  We ignore molecular mass/diameter considerations.
         */
        boolean source = isSourceBlock(world, x, y, z); 
        if (!source)
        {
            int y2 = y - densityDir;

            if (world.getBlock(x, y2, z) == this)
            {
                expQuanta = getQuantaValue(world, x, y2, z);
                if (quantaRemaining < quantaPerBlock - 1 || 
                	isSourceBlock(world, x, y2, z) ||
                	hasSmallerQuanta(world, x - 1, y2, z,     expQuanta) ||
                	hasSmallerQuanta(world, x + 1, y2, z,     expQuanta) ||
                	hasSmallerQuanta(world, x,     y2, z - 1, expQuanta) ||
                	hasSmallerQuanta(world, x,     y2, z + 1, expQuanta)) 
                {
                	expQuanta--;
                }
            }
            else
            {
            	int maxQuanta = -100;
                maxQuanta = getLargerQuanta(world, x - 1, y, z,     maxQuanta);
                maxQuanta = getLargerQuanta(world, x + 1, y, z,     maxQuanta);
                maxQuanta = getLargerQuanta(world, x,     y, z - 1, maxQuanta);
                maxQuanta = getLargerQuanta(world, x,     y, z + 1, maxQuanta);

                expQuanta = maxQuanta - 1;
            }

            // decay calculation
            if (expQuanta != quantaRemaining)
            {
                quantaRemaining = expQuanta;

                if (expQuanta <= 0)
                {
                    world.setBlock(x, y, z, Blocks.air);
                }
                else
                {
                    world.setBlockMetadataWithNotify(x, y, z, quantaPerBlock - expQuanta, 3);
                    world.scheduleBlockUpdate(x, y, z, this, tickRate);
                    world.notifyBlocksOfNeighborChange(x, y, z, this);
                }
            }
            
            if (world.getBlock(x, y + densityDir, z) == this) {
            	return;
            }
        }
        else if (source)
        {
        	world.setBlockMetadataWithNotify(x, y, z, 0, 2);
        }
 
        boolean[] canFlowLaterally = new boolean[] { 
        		canDisplaceInDirection(world, x, y, z, ForgeDirection.VALID_DIRECTIONS[2]),
        		canDisplaceInDirection(world, x, y, z, ForgeDirection.VALID_DIRECTIONS[3]),
        		canDisplaceInDirection(world, x, y, z, ForgeDirection.VALID_DIRECTIONS[4]),
        		canDisplaceInDirection(world, x, y, z, ForgeDirection.VALID_DIRECTIONS[5])
		};
        int numFeasibleLateralFlows = 0;
        for (boolean dir : canFlowLaterally) {
        	if (dir) numFeasibleLateralFlows++;
        }
        boolean flowVertically = shouldGasFlowVertically(world, x, y, z, rand, numFeasibleLateralFlows);
        int numLateralFlows = flowVertically ? 1 : 2;
        boolean diffuse = shouldGasDiffuse(world, x, y, z, rand, numFeasibleLateralFlows, numLateralFlows);
        
        int flowMeta = Math.max(1, quantaPerBlock - quantaRemaining + 
        		(diffuse || quantaRemaining < quantaPerBlock - 1 ? 1 : 0));
        if (flowMeta >= quantaPerBlock) { // dissipated
        	return;
        }
        
        if (!diffuse) {
        	numLateralFlows--;
        }
        
        numLateralFlows = Math.min(numLateralFlows, numFeasibleLateralFlows);
        if (!source && !this.canDisplace(world, x, y + densityDir, z)) {
        	Geologica.log.info("cannot displace block above: lateral flows: " + numLateralFlows + "(" + numFeasibleLateralFlows + ") " + flowVertically);
        }
        
        while(numLateralFlows > 0) {
        	int i = rand.nextInt(canFlowLaterally.length);
        	if (canFlowLaterally[i]) {
        		ForgeDirection dir = ForgeDirection.VALID_DIRECTIONS[i+2];
        		flowIntoBlock(world, x + dir.offsetX, y, z + dir.offsetZ, flowMeta);
        		canFlowLaterally[i] = false;
        		numLateralFlows--;
        	}
        }
        
        if (flowVertically)
        {
        	flowIntoBlock(world, x, y + densityDir, z, flowMeta);
        }
    }

	private boolean canDisplaceInDirection(World world, int x, int y, int z, ForgeDirection dir) {
		return this.canDisplace(world, x + dir.offsetX, y, z + dir.offsetZ);
	}

	private boolean shouldGasDiffuse(World world, int x, int y, int z, Random rand, 
			int numFeasibleFlows, int minFlows) {
		double steamTemperature = Compounds.H2O.getVaporization().getTemperature();
        float pDiffuse = (float)(Math.sqrt(this.temperature) / 
        		(Math.sqrt(steamTemperature) * 3));
        return rand.nextFloat() < pDiffuse && numFeasibleFlows >= minFlows;
	}

	private boolean shouldGasFlowVertically(World world, int x, int y, int z,
			Random rand, int numFeasibleLateralFlows) {
		// Forge density units are too large for gases when rounding to integer
		double materialDensity = this.fluid.getMaterialDensity(); 
		double conc = this.getQuantaValue(world, x, y, z) / (double)this.quantaPerBlock;
		// FIXME: this air density will need to be the atmospheric density of the dimension
		double pVertical = Math.abs(conc*(Constants.AIR_DENSITY - conc*materialDensity)) / 
				Constants.AIR_DENSITY;
		boolean laterallyConstrained = numFeasibleLateralFlows == 0;
        boolean flowVertically = canDisplace(world, x, y + densityDir, z) && 
        		(rand.nextFloat() < 2*pVertical || laterallyConstrained);
		return flowVertically;
	}

	private int getDensityAt(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		int density = Integer.MAX_VALUE;
		if (block instanceof BlockFluidBase) {
			density = ((BlockFluidBase)block).getFluid().getDensity(world, x, y, z);
		}
		return density;
	}

	private boolean hasSmallerQuanta(IBlockAccess world, int x, int y, int z, int quanta) {
    	int other = this.getQuantaValue(world, x, y, z);
    	return other > 0 && other < quanta;
    }
}