package com.alpeb

import com.phidgets._
import com.phidgets.event._
import java.io._
import java.util.Timer
import java.util.TimerTask
import javax.sound.sampled._

object Main extends App {
  val SECONDS = 5 * 60
  val INPUT_INDEX = 0
  val SOUND = new BufferedInputStream(getClass.getResourceAsStream("/sample14.wav"))
  val ais = AudioSystem.getAudioInputStream(SOUND)
  val clip = AudioSystem.getClip

  var timerTask: Option[TimerTask] = None
  var open = true

  clip.open(ais)
  println(Phidget.getLibraryVersion)

  val ik = new InterfaceKitPhidget
  ik.addAttachListener(new AttachListener() {
    override def attached(ae: AttachEvent) = {
      println(s"Attachment of $ae")
      val phidget = ae.getSource.asInstanceOf[InterfaceKitPhidget]
      open = phidget.getInputState(INPUT_INDEX)
      changeState(!open)
    }
  })

  ik.addDetachListener(new DetachListener() {
    override def detached(ae: DetachEvent) = {
      println(s"Detachment of $ae");
    }
  });

  ik.addErrorListener(new ErrorListener() {
    override def error(ee: ErrorEvent) = println(ee)
  });

  ik.addInputChangeListener(new InputChangeListener() {
    override def inputChanged(oe: InputChangeEvent) = {
      if (oe.getIndex == INPUT_INDEX) changeState(!oe.getState)
    }
  })

  ik.openAny
  println("Waiting for InterfaceKit attachment...")
  ik.waitForAttachment
  println(ik.getDeviceName)
  System.in.read()

  private def changeState(newOpen: Boolean) = {
    if (open && !newOpen) {
      println("Gate is closed")
      timerTask.foreach(_.cancel())
      timerTask = None
      clip.stop
    } else if (!open && newOpen) {
      println("Gate is **OPEN**")
      timerTask = Some(new TimerTask() {
        override def run() = {
          try {
            println("** SOUND ALARM **")
            clip.loop(Clip.LOOP_CONTINUOUSLY)
          } catch {
            case e: Exception => println(s"** OOPS: $e")
          }
        }
      })
      new Timer().schedule(timerTask.get, SECONDS * 1000)
    }
    open = newOpen
  }
}
