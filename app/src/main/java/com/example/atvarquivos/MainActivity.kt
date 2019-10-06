package com.example.atvarquivos

import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.widget.ActionMenuView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.lang.Exception
import java.lang.StringBuilder
import java.nio.Buffer

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSalvar.setOnClickListener {
            salvar(findViewById(rgOpcoesMemoria.checkedRadioButtonId))
        }

        btnLer.setOnClickListener {
            carregar(findViewById(rgOpcoesMemoria.checkedRadioButtonId))
        }

    }


    private fun salvar(rbChecked : RadioButton){
        val conteudo = edtText.text

        if(rbChecked == rbMemoriaInterna) {
            salvarArquivoMemoriaInterna(conteudo.toString())
        } else if (rbChecked == rbMemoriaExternaPvt) {
            salvarArquivoSdCard(conteudo.toString(), true)
        } else if (rbChecked == rbMemoriaExternaPub) {
            salvarArquivoSdCard(conteudo.toString(), false)
        }
    }

    private fun carregar(rbChecked: RadioButton) {
        var conteudo : String = "ARQUIVO NÃO FOI CARREGADO AINDA"
        if(rbChecked == rbMemoriaInterna) {
            conteudo = lerArquivoMemoriaInterna()
        } else if (rbChecked == rbMemoriaExternaPvt) {
            conteudo = lerArquivoSdCard(true)
        } else if (rbChecked == rbMemoriaExternaPub) {
            conteudo = lerArquivoSdCard(false)
        }
        tvConteudo.text = conteudo
    }

    private fun save(txt: String, fos : FileOutputStream){
        val lines = TextUtils.split(txt, "\n")
        val writer = PrintWriter(fos)

        for(l in lines){
            writer.println(l)
        }

        writer.flush()
        writer.close()
        fos.close()
    }

    private fun load(fis: FileInputStream) : String {
        val reader = BufferedReader(InputStreamReader(fis))
        val sb = StringBuilder()

        do{
            val line = reader.readLine() ?: break
            if(sb.isNotEmpty()){
                sb.append(('\n'))
            }
            sb.append(line)
        } while (true)

        reader.close()
        fis.close()

        return sb.toString()
    }

    private fun salvarArquivoMemoriaInterna(conteudo: String){
        try{
            val fos = openFileOutput(NAME_ARQUIVO_INTERNO, Context.MODE_PRIVATE)
            save(conteudo, fos)
        } catch (e: java.lang.Exception) {
            Log.e(ERROR_FILE, ERROR_FILE_MSG)
        }
    }

    private fun lerArquivoMemoriaInterna() : String {
        try{
            val fis = openFileInput(NAME_ARQUIVO_INTERNO)
            return load(fis)
        } catch (e: java.lang.Exception) {
            Log.e(ERROR_FILE, ERROR_FILE_MSG)
            throw e
        }
    }
    private fun getExternalDir(diretorioPrivado: Boolean) : File? {
        if(diretorioPrivado) {
            return getExternalFilesDir(null)
        } else {
            return Environment.getExternalStorageDirectory()
        }
    }

    public fun salvarArquivoSdCard(conteudo : String, diretorioPrivado : Boolean){

        val possuiPermissao = checkPermissoesArmazenamento(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            RC_STORAGE_PERMISSION
        )

        if(!possuiPermissao){
            return
        }

        val state = Environment.getExternalStorageState()
        if(state == Environment.MEDIA_MOUNTED){
            val myDir = getExternalDir(diretorioPrivado)
            try{
                if(!myDir?.exists()!!){
                    myDir.mkdir()
                }

                val myFile = File(myDir, NAME_ARQUIVO_EXTERNO)

                if(!myFile.exists()){
                    myFile.createNewFile()
                }

                val fos = FileOutputStream(myFile)
                save(conteudo, fos)
            } catch (e: Exception) {
                Log.e(ERROR_FILE, ERROR_FILE_MSG)
            }

        } else {
            Log.e(ERROR_FILE, ERROR_SD_CARD_INDISPONIVEL)
        }
    }

    private fun lerArquivoSdCard(diretorioPrivado: Boolean) : String {

        val possuiPermissao = checkPermissoesArmazenamento(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            RC_STORAGE_PERMISSION
        )

        if(!possuiPermissao){
            throw Exception(ERROR_FILE_SEM_PERMISSAO)
        }

        val state = Environment.getExternalStorageState()
        var conteudo : String = ""
        if(state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY){
            val myDir = getExternalDir(diretorioPrivado)

            if(myDir?.exists()!!){
                val myFile = File(myDir, NAME_ARQUIVO_EXTERNO)
                if(myFile.exists()){
                    try {
                        myFile.createNewFile()
                        val fis = FileInputStream(myFile)
                        conteudo = load(fis)
                    } catch (e: Exception) {
                        Log.e(ERROR_FILE, ERROR_FILE_MSG)
                    }
                } else {
                    Log.e(ERROR_FILE, ERROR_FILE_ARQUIVO_NAO_ENCONTRADO)
                }
            } else {
                Log.e(ERROR_FILE, ERROR_FILE_DIRETORIO_NAO_ENCONTRADO)
            }
        } else {
            Log.e(ERROR_FILE, ERROR_SD_CARD_INDISPONIVEL)
        }
        return conteudo
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            RC_STORAGE_PERMISSION -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showToast("Permissão concedida")
                } else {
                    showToast("Permissão negada")
                }
            }
        }
    }

    private fun checkPermissoesArmazenamento(permission : String, requestCode : Int) : Boolean {
        if(ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, permission)){
                showToast("Você precisa habilitar essa permissão para ler ou salvar arquivo no diretório público")
            }
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            return false
        } else {
            return true
        }
    }

    private fun showToast(txt : String) {
        Toast.makeText(this, txt, Toast.LENGTH_SHORT).show()
    }

    companion object {
        val RC_STORAGE_PERMISSION = 0
        val ERROR_FILE = "ERROR_FILE"
        val ERROR_FILE_MSG = "Erro ao salvar arquivo"
        val ERROR_SD_CARD_INDISPONIVEL = "SD Card indisponível"
        val ERROR_FILE_SEM_PERMISSAO = "Não está autorizado a salvar no arquivo"
        val ERROR_FILE_DIRETORIO_NAO_ENCONTRADO = "Diretorio não encontrado"
        val ERROR_FILE_ARQUIVO_NAO_ENCONTRADO = "Arquivo não encontrado"
        val NAME_ARQUIVO_INTERNO = "arquivo_interno.txt"
        val NAME_ARQUIVO_EXTERNO = "arquivo_externo.txt"
    }

}
