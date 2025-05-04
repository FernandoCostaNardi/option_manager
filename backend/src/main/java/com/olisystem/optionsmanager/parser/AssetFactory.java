package com.olisystem.optionsmanager.parser;

import com.olisystem.optionsmanager.model.Asset.Asset;
import com.olisystem.optionsmanager.model.Asset.AssetType;
import org.json.JSONArray;
import org.json.JSONObject;

public class AssetFactory {
  public static Asset fromJson(JSONObject json) throws Exception {
    JSONArray results = json.getJSONArray("results");
    if (results.length() > 0) {
      JSONObject ativo = results.getJSONObject(0);
      Asset info = new Asset();
      info.setCode(ativo.getString("symbol"));
      info.setName(ativo.optString("longName", "Nome desconhecido"));
      info.setUrlLogo(ativo.optString("logourl", ""));
      info.setType(AssetType.STOCK);
      return info;
    }
    throw new Exception("Nenhum resultado encontrado no JSON para o ativo.");
  }
}
