package gov.nih.mipav.view.renderer.WildMagic.AAM;

/**
 * This is the Java modified version of C++ active appearance model API
 * (AAM_API). It is modified with a subset of required functions for automatic
 * MRI prostate segmentation. 
 * 
  * AAM-API LICENSE  -  file: license.txt
 * 
 * This software is freely available for non-commercial use such as
 * research and education. Please see the full disclaimer below. 
 * 
 * All publications describing work using this software should cite 
 * the reference given below. 
 * 	
 * Copyright (c) 2000-2003 Mikkel B. Stegmann, mbs@imm.dtu.dk
 * 
 * 
 * IMM, Informatics & Mathematical Modelling
 * DTU, Technical University of Denmark
 * Richard Petersens Plads, Building 321
 * DK-2800 Lyngby, Denmark
 * 
 * http://www.imm.dtu.dk/~aam/
 * 
 * 
 * 
 * REFERENCES
 * 
 * Please use the reference below, when writing articles, reports etc. where 
 * the AAM-API has been used. A draft version the article is available from 
 * the homepage. 
 * 
 * I will be happy to receive pre- or reprints of such articles.
 * 
 * /Mikkel
 * 
 * 
 * -------------
 * M. B. Stegmann, B. K. Ersb�ll, R. Larsen, "FAME -- A Flexible Appearance 
 * Modelling Environment", IEEE Transactions on Medical Imaging, IEEE, 2003
 * (to appear)
 * -------------
 * 
 *
 * 
 * 3RD PART SOFTWARE
 * 
 * The software is partly based on the following libraries:
 * 
 * - The Microsoft(tm) Vision Software Developers Kit, VisSDK
 * - LAPACK
 * 
 *
 * DISCLAIMER
 * 
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the author be held liable for any damages arising from the
 * use of this software.
 * 
 * Permission is granted to anyone to use this software for any non-commercial 
 * purpose, and to alter it, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not claim
 *  that you wrote the original software. 
 *
 * 2. Altered source versions must be plainly marked as such, and must not be 
 *  misrepresented as being the original software.
 * 
 * 3. This notice may not be removed or altered from any source distribution.
 * 
 * --
 *
 * No guarantees of performance accompany this software, nor is any 
 * responsibility assumed on the part of the author or IMM. 
 * 
 * This software is provided by Mikkel B. Stegmann and IMM ``as is'' and any 
 * express or implied warranties, including, but not limited to, the implied 
 * warranties of merchantability and fitness for a particular purpose are 
 * disclaimed. In no event shall IMM or Mikkel B. Stegmann be liable for any 
 * direct, indirect, incidental, special, exemplary, or consequential damages
 * (including, but not limited to, procurement of substitute goods or services;
 * loss of use, data, or profits; or business interruption) however caused and 
 * on any theory of liability, whether in contract, strict liability, or tort 
 * (including negligence or otherwise) arising in any way out of the use of 
 * this software, even if advised of the possibility of such damage.
 * 
 * 
 * 
 *
 * $Revision: 1.4 $ 
 * $Date: 2003/04/23 14:49:15 $ 
 * 
 * 
 * Console mode M for movies.
 * 
 * @author Ruida Cheng
 * 
 */
public class CAAMConsoleModeM extends CAAMConsoleMode {

	/**
	 * Constructor. Console mode M for movies.
	 * 
	 * @param progname
	 *            program name
	 */
	public CAAMConsoleModeM(final String progname) {

		m_ProgName = progname;
		m_NMinParam = 2;
		m_NMaxParam = 6;

		m_Name = new String("m");
		m_Usage = new String(
				"<model.amf> <all|shape|texture|combined> [#modes] [#frames] [white|black*] [range]");
		m_Desc = new String("Writes Active Appearance Model mode movies.");
		m_LongDesc = new String(
				"This mode documents the given AAM by generating movies showing the shape,\n"
						+ "texture and combined variation resulting from the PCAs.\n"
						+ "\nOutput are written in current dir.\n\n"
						+ "[#modes]        : The number of model modes to render (3).\n"
						+ "[#frames]       : The frames to render each mode in (10).\n"
						+ "[white|black]   : Background colour (black).\n"
						+ "[range]         : Parameter range (3).\n");
	}

	/**
	 * C style anchor to invoke the M console mode. Being called from the AAM
	 * console.
	 * 
	 * @param argc
	 *            number of augments
	 * @param argv
	 *            augments array
	 * @return nothing
	 */
	public int Main(int argc, String[] argv) {

		// call base implementation
		super.Main(argc, argv);

		boolean writeMovie = false, writeImages = true;

		// setup input parameters
		String inModel = argv[1];
		String movieType = argv[2];
		int nModes = argc >= 3 ? Integer.valueOf(argv[3]) : 3;
		int nSteps = argc >= 4 ? Integer.valueOf(argv[4]) / 2 : 10;
		boolean bWhiteBG = argc >= 5 ? argv[5].equals("white") : false;
		double range = argc >= 6 ? Double.valueOf(argv[6]) : 3;

		// test movie type
		if (!(movieType.equals("all") || movieType.equals("shape")
				|| movieType.equals("texture") || movieType.equals("combined"))) {

			System.err.println("Error: Movie type '" + movieType
					+ "' is not supported.\n");
			PrintUsage();
			return 1;
		}

		C_AAMMODEL aam = new C_AAMMODEL();

		// read model
		boolean ok = aam.ReadModel(inModel);

		if (!ok) {

			System.err.println("Could not read model file '" + inModel
					+ "'. Exiting...");
			System.exit(1);
		}

		System.err.printf("Generating movie type '" + movieType + "'...");

		// make movie object
		CAAMVisualizer AAMvis = new CAAMVisualizer(aam);

		if (movieType.equals("texture") || movieType.equals("all")) {

			AAMvis.TextureMovie("texture", nModes, range, nSteps, bWhiteBG);
		}

		if (movieType.equals("shape") || movieType.equals("all")) {

			AAMvis.ShapeMovie("shape", nModes, range, nSteps, bWhiteBG);
		}

		if (movieType.equals("combined") || movieType.equals("all")) {

			AAMvis.CombinedMovie("combined", nModes, range, nSteps, bWhiteBG);
		}

		// we're done
		return 0;
	}

}