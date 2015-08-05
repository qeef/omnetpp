//==========================================================================
//   COSGCANVAS.H  -  header for
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2015 Andras Varga
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __OMNETPP_COSGCANVAS_H
#define __OMNETPP_COSGCANVAS_H

#include "cownedobject.h"
#include "ccanvas.h"

#ifdef WITH_OSG

// don't include OSG headers yet
namespace osg { class Node; }
namespace osgEarth { class Viewpoint; }

NAMESPACE_BEGIN

#define OMNETPP_OSGCANVAS_VERSION  0x20150721  //XXX identifies canvas code version until API stabilizes

/**
 * Wraps an OpenSceneGraph scene, allowing 3D visualization in graphical user
 * interfaces that support it (currently Qtenv). This class only wraps an OSG
 * scene (as an osg::Node* pointer) and some visualization hints, other
 * tasks like setting up a 3D viewer window are taken care of by the OMNeT++
 * user interface code (Qtenv). The scene graph can be assembled using the OSG
 * API, e.g. using osgDB::readNodeFile() or creating and adding nodes directly.
 *
 * Since OpenSceneGraph is not a mandatory part of OMNeT++, it is recommended
 * that you surround your OSG code with #ifdef HAVE_OSG.
 *
 * @ingroup Canvas
 */
class SIM_API cOsgCanvas : public cOwnedObject
{
    public:
        enum ViewerStyle { STYLE_GENERIC, STYLE_EARTH };
        typedef cFigure::Color Color;
        enum CameraManipulatorType { CAM_AUTO, CAM_TRACKBALL, CAM_EARTH };

    protected:
        osg::Node *scene;  // reference counted

        ViewerStyle viewerStyle;

        // OSG viewer hints
        Color clearColor;
        CameraManipulatorType cameraManipulatorType;
        double fieldOfViewAngle;  // a.k.a. fovy, see OpenGL gluPerspective
        double zNear; // see OpenGL gluPerspective
        double zFar;  // see OpenGL gluPerspective

        // osgEarth-specific viewer hints
        osgEarth::Viewpoint *viewpoint; // never nullptr

    private:
        void copy(const cOsgCanvas& other);

    public:
        /** @name Constructors, destructor, assignment */
        //@{
        cOsgCanvas(const char *name=nullptr, ViewerStyle viewerStyle=STYLE_GENERIC, osg::Node *scene=nullptr);
        cOsgCanvas(const cOsgCanvas& other) : cOwnedObject(other) {copy(other);}
        virtual ~cOsgCanvas();
        cOsgCanvas& operator=(const cOsgCanvas& other);
        //@}

        /** @name Redefined cObject member functions. */
        //@{
        virtual cOsgCanvas *dup() const override {return new cOsgCanvas(*this);}
        virtual std::string info() const override;
        //@}

        /** @name OSG scene. */
        //@{
        /**
         * Set the 3D scene to be displayed. Note that osg::Node implements
         * reference counting, and setScene() increments the reference count.
         */
        virtual void setScene(osg::Node *scene);

        /**
         * Return the 3D scene to be displayed.
         */
        virtual osg::Node *getScene() const {return scene;}
        //@}

        /** @name Hints for the OSG viewer. */
        //@{
        /**
         * TODO
         */
        void setViewerStyle(ViewerStyle viewerStyle) {this->viewerStyle = viewerStyle;}

        /**
         * TODO
         */
        ViewerStyle getViewerStyle() const {return viewerStyle;}

        /**
         * Set the color hint for the background behind the scene.
         */
        void setClearColor(Color clearColor) {this->clearColor = clearColor;}

        /**
         * Return the color hint for background behind the scene.
         */
        Color getClearColor() const {return clearColor;}

        /**
         * Set the camera manipulator type hint. The camera manipulator
         * determines how the camera reacts to mouse/keyboard actions.
         */
        void setCameraManipulatorType(CameraManipulatorType manipulator) {this->cameraManipulatorType = manipulator;}

        /**
         * Return the camera manipulator type hint.
         */
        CameraManipulatorType getCameraManipulatorType() const {return cameraManipulatorType;}

        /**
         * Set the field of view angle hint, in degrees, in the y direction.
         */
        void setFieldOfViewAngle(double fieldOfViewAngle) {this->fieldOfViewAngle = fieldOfViewAngle;}

        /**
         * Return the field of view angle hint, in degrees, in the y direction.
         */
        double getFieldOfViewAngle() const {return fieldOfViewAngle;}

        /**
         * Set the distance hint from the viewer to the near clipping plane
         * (always positive).
         */
        void setZNear(double zNear) {this->zNear = zNear;}

        /**
         * Return the distance hint from the viewer to the near clipping plane.
         */
        double getZNear() const {return zNear;}

        /**
         * Set the distance hint from the viewer to the far clipping plane
         * (always positive).
         */
        void setZFar(double zFar) {this->zFar = zFar;}

        /**
         * Return the distance from the viewer to the far clipping plane.
         */
        double getZFar() const {return zFar;}

        /**
         * Set all perspective-related viewer hints together.
         * @see setFieldOfViewAngle(), setZNear(), setZFar()
         */
        void setPerspective(double fieldOfViewAngle, double zNear, double zFar);
        //@}

        /** @name osgEarth-related viewer hints. */
        //@{
        /**
         * Sets the initial viewpoint hint.
         */
        void setEarthViewpoint(const osgEarth::Viewpoint& viewpoint);

        /**
         * Returns the initial viewpoint hint.
         */
        const osgEarth::Viewpoint& getEarthViewpoint() const {return *viewpoint;}

        //TODO more osgEarth-related hints

        //@}

};

NAMESPACE_END

#endif // WITH_OSG


#endif