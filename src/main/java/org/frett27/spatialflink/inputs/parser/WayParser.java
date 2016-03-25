package org.frett27.spatialflink.inputs.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.frett27.spatialflink.inputs.OSMContext;
import org.frett27.spatialflink.inputs.parser.NodeParser.DenseNodeState;
import org.frett27.spatialflink.inputs.parser.NodeParser.NodesState;
import org.frett27.spatialflink.model.RelatedObject;
import org.frett27.spatialflink.model.Relation;
import org.frett27.spatialflink.model.WayEntity;

import crosby.binary.Osmformat.PrimitiveBlock;
import crosby.binary.Osmformat.PrimitiveGroup;
import crosby.binary.Osmformat.Way;

public class WayParser extends Parser<WayEntity> {

	private OSMContext ctx;
	private List<PrimitiveGroup> groups;

	public WayParser(PrimitiveBlock block) {

		assert block != null;

		this.ctx = createOSMContext(block);

		groups = new ArrayList<PrimitiveGroup>(block.getPrimitivegroupList());

	}

	public class WayParserDecomposer {

		private ArrayList<Way> left;

		public WayParserDecomposer(List<Way> ways) {
			assert ways != null;
			left = new ArrayList<Way>(ways);
		}

		public WayEntity next() {

			if (left.size() == 0)
				return null;

			Way w = left.get(0);
			left.remove(0);

			long lastRef = 0;
			List<Long> l = w.getRefsList();

			long[] refids = new long[l.size()];
			// liste des references ...

			List<RelatedObject> rels = null;

			int cpt = 0;
			for (Long theid : l) {
				if (rels == null)
					rels = new ArrayList<RelatedObject>();
				lastRef += theid;
				refids[cpt++] = lastRef;
				RelatedObject relo = new RelatedObject();
				relo.relatedId = lastRef;
				rels.add(relo);
			}

			Map<String, String> flds = null;

			for (int i = 0; i < w.getKeysCount(); i++) {
				String k = ctx.getStringById(w.getKeys(i));
				String v = ctx.getStringById(w.getVals(i));
				if (flds == null) {
					flds = new HashMap<String, String>();
				}
				flds.put(k, v);
			}

			long wid = w.getId();

			WayEntity r = new WayEntity();
			r.fields = flds;
			r.id = wid;
			r.relatedObjects = (rels == null ? null : rels.toArray(new RelatedObject[rels.size()]));

			return r;
		}

	}

	private PrimitiveGroup current;

	private WayParserDecomposer decomposer;

	@Override
	public WayEntity next() throws Exception {

		if (groups == null) { // end of group read
			return null;
		}

		// next primitive group
		if (current == null) {

			if (groups.size() == 0) {
				groups = null;
				return null;
			}

			assert groups.size() > 0;
			PrimitiveGroup g = groups.get(0);
			current = g;
			decomposer = new WayParserDecomposer(g.getWaysList());
			groups.remove(0);

		}

		assert decomposer != null;

		WayEntity w = decomposer.next();

		if (w == null) {
			// next group
			current = null;
			return next();
		}

		return w;
	}

}
